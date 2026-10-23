# Proposal

## Why

Chunks are written by `TicketIndexer` with JDBC. Each comment is a section, the text starts with a section name, and a missing assignee is stored as null. Ingestion should build Spring AI documents from the title, one chunk per comment, and the resolution notes, then replace that ticket's vectors in the `VectorStore`.

## What Changes

- Add `TicketIngestionService` in `tickets.ai`. It builds one chunk for the title plus description, one chunk per comment, and one resolution chunk when notes are present.
- **BREAKING:** The description is no longer split every 500 words. `partIndex` and `groupIndex` are not stored.
- Each chunk is a Spring AI `Document`. The text starts with the ticket id and the title:

```text
Ticket: TKT-1001
Title: Export timeout
Invoice export fails for quarterly reports.
```

- Metadata on every chunk is `ticketId`, `status`, `priority`, `assignee`, `category`, and `chunkType`. `chunkType` is `description`, `comment`, or `resolution`. A missing assignee is `""`.
- Re-ingestion deletes every stored chunk whose metadata `ticketId` matches, then inserts the new documents.
- A successful create, update, comment, or allowed status change still re-ingests that ticket in the same transaction. Failure still rolls back and returns `503` with message `Ticket could not be indexed`.
- Add `backfill` to ingest every existing ticket, one ticket per transaction, and `POST /api/ai/reindex` to call it. The response is `200` with `indexed` and `failed`. One failure does not stop the rest.

Assumption: each comment is one group, so each comment is one chunk. The reindex route has no auth, matching the other ticket routes. `indexed` counts tickets that committed. `failed` lists the ids that rolled back.

## Capabilities

### New Capabilities

- `rag-ingestion`: How a ticket becomes vector documents, how those documents are replaced, and how an operator reindexes every ticket.

### Modified Capabilities

## Impact

- `tickets.ai.TicketIndexer` and `OllamaEmbedder` write `vector_store` with JDBC. `TicketService` calls the indexer after a successful write.
- `pom.xml` has no Spring AI dependency. `application.yml` has an `ai:` block at the root, so Boot does not bind it as `spring.ai`.
- `spec/rag-ingestion.md` still specifies section names, `sectionType`, a null assignee, the 500-word split, and startup flag `app.rag.backfill`. This change rewrites that document.
- Depends on a stored ticket title from `add-ticket-persistence`. The tickets re-index requirement stays: a replaced resolution must not remain searchable.
- Default tests keep using a fake store. They do not call Ollama.
