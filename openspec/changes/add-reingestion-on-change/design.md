# Design

## Context

`TicketService` calls `TicketIndexer.reindex` inside `@Transactional` create, comment, and patch methods. `reindex` catches a failure and throws `ApiException` 503, which rolls the ticket write back. See proposal.md for why that moves to after commit. `spec/rag-ingestion.md` still describes the in-transaction 503 path.

## Goals / Non-Goals

**Goals:**

- Publish one event per committed ticket change and ingest only after that commit.
- Log an embedding failure with the ticket id and still return the write's success status.
- Leave `CANCELLED` and `CLOSED` tickets searchable with the new status on their chunks.

**Non-Goals:**

- Changing chunk text, metadata keys, or `POST /api/ai/reindex`.
- Ingesting a validation failure or a forbidden transition.
- Making the listener asynchronous.

## Decisions

### Event after commit

`TicketService` publishes `TicketChangedEvent` with the ticket id after the row write, and no longer calls the indexer itself. `tickets.ai` listens with `@TransactionalEventListener(phase = AFTER_COMMIT)`. Spring skips that listener when the ticket transaction rolls back.

The listener loads the ticket from the database and builds embeddings before it deletes old chunks. The delete and insert then run in a new transaction. An exception is caught and logged at error level with the ticket id. It is not rethrown, so the HTTP status stays `201` or `200`.

The listener is synchronous. The client waits for the ingest attempt, and other transactions can already see the committed ticket while embeddings run.

Alternative: keep ingest inside the ticket transaction. Rejected because a rollback would be the only way to avoid embedding uncommitted rows, and the request asks for the opposite failure policy.

Alternative: `@Async`. Rejected because the success path can still finish the replace before the response, and an async failure is harder to assert in the existing tests.

### Cancelled tickets use the same replace

`CANCELLED` is not a special delete. The listener loads the committed row, so the new chunks carry `status` `CANCELLED`. The previous chunks are removed only after the new embeddings exist. `CLOSED` stays on that same path.

A forbidden transition returns `409` before any event is published.

## Risks / Trade-offs

- [The ticket row is visible before the new chunks are stored] → The listener still runs before the controller returns, on the success path. A concurrent reader can see the new row with the previous chunks during that wait.
- [A failed embed leaves stale chunks and a successful HTTP response] → The log line carries the ticket id. `POST /api/ai/reindex` from `add-rag-ingestion` is the repair. This change no longer returns `503` for an embed failure.
- [`add-rag-ingestion` still describes an in-transaction 503] → This change is the write-path timing. That change should call the same listener rather than put ingest back inside `TicketService`.

## Migration Plan

1. Replace the direct `reindex` calls with the event and the after-commit listener.
2. Update `spec/rag-ingestion.md` so the failure section matches the log-and-continue behavior.
3. Roll back by calling the indexer inside the transaction again and restoring the `503` mapping.

## Open Questions

None. Each committed create, update, comment, and allowed status change publishes one event. Embeddings are computed before the old chunks are deleted.
