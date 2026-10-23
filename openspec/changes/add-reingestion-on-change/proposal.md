# Proposal

## Why

`TicketService` embeds a ticket inside the same transaction as the row write. A failed embed rolls the ticket back and the client receives `503`. Ingestion should run only after that transaction commits, and an embedding failure should leave the user's success response in place.

## What Changes

- **BREAKING:** A successful create, update, comment, or allowed status change, including close and cancel, publishes `TicketChangedEvent` from `TicketService`. A `@TransactionalEventListener(phase = AFTER_COMMIT)` ingests that ticket. A rolled-back write does not ingest.
- **BREAKING:** If embedding or the vector replace fails, the error is logged with the ticket id and the client still receives the success status of the write. The previous chunks remain. The write no longer returns `503` `Ticket could not be indexed`.
- A `CANCELLED` ticket stays in the index. Its chunk metadata `status` becomes `CANCELLED`. Close stays searchable the same way, with `status` `CLOSED`.
- A validation failure or a forbidden status change does not publish the event.

Assumption: `CANCELLED` uses the same delete-then-insert replace as any other committed change. Embeddings are built before that delete, so a failed embed does not remove the previous chunks.

## Capabilities

### New Capabilities

### Modified Capabilities

- `tickets`: Re-index runs after the ticket transaction commits. An embedding failure does not fail the user's request. Cancelled tickets stay indexed with updated status metadata.

## Impact

- `TicketService.create`, `patch`, and `addComment` call `TicketIndexer.reindex` before the transaction commits and turn a failure into `503`.
- `spec/rag-ingestion.md` says the indexer runs before commit, the vector writes share that transaction, and a failure returns `503`. This change rewrites those sections.
- The open change `add-rag-ingestion` also keeps write-path ingest inside the ticket transaction and returns `503`. This change replaces that timing and failure policy. Chunk shape stays with `add-rag-ingestion`.
- No new dependency. The listener calls the ingest component that already exists.
