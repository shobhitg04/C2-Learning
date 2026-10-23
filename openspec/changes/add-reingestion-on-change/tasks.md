# Tasks

## 1. After-commit ingest

- [ ] 1.1 Publish `TicketChangedEvent` from `TicketService` after a committed create, comment, assignee or resolution-note update, and allowed status change, including close and cancel. Do not publish it for a validation failure or a forbidden transition. Handle it with `@TransactionalEventListener(phase = AFTER_COMMIT)`, reload the ticket, and ingest only then. Verify a rolled-back create writes no chunks, and a forbidden status change leaves the existing chunk ids unchanged.
- [ ] 1.2 On an embedding or replace failure, log the error with the ticket id, keep the previous chunks, and return the write's success status. Build embeddings before deleting old chunks. Verify a valid create whose embed fails still returns `201`, the ticket row exists, and the previous chunks remain.
- [ ] 1.3 Re-ingest a ticket that becomes `CANCELLED` so its chunks stay searchable and their status metadata is `CANCELLED`. Update the trigger and failure sections of `spec/rag-ingestion.md`, including the trade-off against rolling back the ticket write. Verify an `OPEN` to `CANCELLED` change leaves a chunk whose status is `CANCELLED`, and that `spec/rag-ingestion.md` no longer tells the client to expect `503` when embedding fails.

## Workflow follow-up

- If `add-rag-ingestion` is applied later, keep this listener as the write-path trigger.
- Archive with `/opsx-archive` after review.
