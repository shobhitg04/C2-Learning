# Tasks

## 1. Vector store

- [ ] 1.1 Confirm the ticket type has a title. If it does not, stop and apply `add-ticket-persistence` first. Add the Spring AI 1.0 BOM, `spring-ai-starter-model-ollama`, and `spring-ai-starter-vector-store-pgvector`. Move the Ollama and PgVector settings under `spring.ai`, with embedding model `nomic-embed-text`, 768 dimensions, cosine distance, and `initialize-schema: false`. Verify `./mvnw -q -DskipTests compile` from `backend/` exits 0.

## 2. Ingestion service

- [ ] 2.1 Implement `tickets.ai.TicketIngestionService` with the chunk prefix `Ticket: {id}` and `Title: {title}`, one description chunk, one chunk per comment, and a resolution chunk only when notes are non-blank. Set `chunkType` and store a missing assignee as `""`. Verify unit tests for a ticket with one comment and no notes (two chunks), a resolution chunk, and no resolution chunk when notes are blank.
- [ ] 2.2 Delete by metadata `ticketId`, then add the new documents, using a fake `VectorStore` in tests. Point `TicketService` at this service and remove `TicketIndexer` and `OllamaEmbedder` when they are unused. Verify a test that a second ingest does not keep the previous resolution text. Update `spec/rag-ingestion.md` to this chunk contract, including the trade-off against the old per-section text, and verify that file no longer requires the 500-word split or `sectionType`.

## 3. Reindex endpoint

- [ ] 3.1 Add `backfill` so each ticket commits or rolls back on its own, in numeric id order. Add `POST /api/ai/reindex` and return `200` with `indexed` and `failed`. Verify an API test where one ticket fails: `indexed` counts only the committed ticket and `failed` contains the other id.

## Workflow follow-up

- Apply `add-ticket-persistence` first when the ticket type has no title.
- Archive with `/opsx-archive` after review.
