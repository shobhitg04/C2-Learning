# Design

## Context

`TicketIndexer` deletes `vector_store` rows with JDBC and inserts embeddings from `OllamaEmbedder`. `TicketService` calls it inside the ticket transaction. `schema.sql` already creates `vector_store` with `vector(768)` and an HNSW cosine index. See proposal.md for the new chunk contract. `application.yml` currently places Ollama and PgVector keys under a root `ai:` key, and `pom.xml` does not depend on Spring AI.

## Goals / Non-Goals

**Goals:**

- Build Spring AI `Document`s in `tickets.ai.TicketIngestionService` and store them through `VectorStore`.
- Replace one ticket's chunks by metadata `ticketId`, and reindex every ticket from `POST /api/ai/reindex`.

**Non-Goals:**

- The ask API, the chat model call, and retrieval top-K.
- Splitting a description every 500 words.
- Adding auth to the reindex route.

## Decisions

### Spring AI owns embed and store

Add the Spring AI 1.0 BOM, `spring-ai-starter-model-ollama`, and `spring-ai-starter-vector-store-pgvector`. Move the existing Ollama and PgVector keys under `spring.ai`. The embedding model stays `nomic-embed-text` at 768 dimensions and cosine distance.

`spring.ai.vectorstore.pgvector.initialize-schema` is `false`. `schema.sql` already creates the table and the HNSW index. Leaving initialization on would race that script.

`TicketIngestionService` depends on `VectorStore`. `add` embeds the documents. `delete` uses a metadata filter `ticketId == id`. `TicketService` calls this service instead of `TicketIndexer`. Remove `TicketIndexer` and `OllamaEmbedder` when nothing else references them.

Alternative: keep JDBC inserts and wrap each row in a `Document` only in memory. Rejected because the request stores through `VectorStore`.

### Chunk text

Every chunk uses the same prefix:

```text
Ticket: TKT-1001
Title: Export timeout
```

The description chunk then contains the description. A comment chunk contains that comment body. A resolution chunk contains the notes. Comments are ordered by `created_at`, then id, and each comment is its own document. `chunkType` is the only type field. Assignee metadata is `""` when the ticket assignee is null.

If the ticket type has no title yet, stop and apply `add-ticket-persistence` before writing chunks.

### Backfill is not the write transaction

`backfill` loads ticket ids in numeric order. Each id runs in its own transaction. A failure is caught, the ticket id is added to `failed`, and the loop continues. `POST /api/ai/reindex` returns `200` and `{ "indexed": 1, "failed": ["TKT-1002"] }`.

A write-path ingest stays inside the ticket transaction. `VectorStore` must participate in that transaction so a failed embed rolls the ticket write back and the client still receives `503`.

Tests use a fake `VectorStore`. The default build does not call Ollama.

## Risks / Trade-offs

- [The PgVector starter expects a different metadata column type than `schema.sql`] → Adjust `schema.sql` only if the starter cannot read the existing `id`, `content`, `metadata`, and `embedding` columns. Keep the HNSW cosine index.
- [`POST /api/ai/reindex` embeds every ticket and has no auth] → Any client who can reach the API can start it. That matches the other routes. One ticket per transaction keeps a single failure from wiping the index.
- [Root `ai:` settings are not Spring AI settings] → Moving them under `spring.ai` is what makes the starter see the model and the dimension. The chat model key can move with them. This change does not call the chat model.

## Migration Plan

1. Add the starters and point `TicketService` at `TicketIngestionService`.
2. Reindex existing tickets through `POST /api/ai/reindex` so old section chunks are replaced.
3. Roll back by restoring `TicketIndexer`. Old chunks remain until the next reindex.

## Open Questions

None. Each comment is one chunk. The reindex response fields are `indexed` and `failed`.
