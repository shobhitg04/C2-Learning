# Design

## Context

See `proposal.md` for why this change exists. The repository has `docker-compose.yml` for `pgvector/pgvector:pg16` and the ticket behavior in `spec/`. It has no application source. The behavior contract for this change is `specs/tickets/spec.md`.

## Goals / Non-Goals

**Goals:**

- One Spring Boot process serves the ticket API and the four ticket screens.
- Status rules are testable without starting Spring.
- A successful ticket write and its chunk replacement commit together.

**Non-Goals:**

- `POST /api/ai/ask`, the assistant panel, and the live retrieval checklist.
- Choosing a value for `spring.ai.ollama.chat.options.model`.
- Backfill on startup.

## Decisions

### Application layout

Put the Spring Boot app in `backend/`. Packages are `tickets.controller`, `tickets.service`, `tickets.repository`, `tickets.domain`, and `tickets.ai`.

The repo root stays the Compose file and `spec/`. Putting Java at the root would mix the app with those files. A second service is unnecessary for four screens.

### Server-rendered screens

Serve `/tickets`, `/tickets/new`, `/tickets/{id}`, and `/tickets/{id}/edit` from the same app. The page checks blank fields before the request and prints `fieldErrors[].message` under the matching field.

A separate browser app would add a toolchain this repo does not have. The screens are a thin client of the ticket API.

### Status policy

`tickets.domain` holds the allowed and forbidden pairs from `spec/state-machine.md` as a pure decision. `tickets.service` applies that decision inside one transaction. The controller does not branch on status.

Putting the pairs only in the service would force the 25 pair tests to boot Spring. Duplicating the pairs in the controller could drift from the domain.

### Ticket ids

Use a PostgreSQL sequence `ticket_id_seq` starting at 1001. The public id is `TKT-` plus that value. `spec/data-model.md` does not name this sequence yet. Apply adds it there.

`max(id) + 1` can assign the same id to two creates. A sequence does not.

### Re-index in the ticket transaction

On a successful create, comment, assignee change, resolution-note change, or allowed status change, delete that ticket's `vector_store` rows and insert the current section chunks before commit. A forbidden change or a validation failure does not call the indexer. Embedding uses Ollama `nomic-embed-text` at 768 dimensions. A description of 500 words or fewer is one chunk. Longer text is split every 500 words with `partIndex` starting at 1.

Indexing after commit can return success while search still sees the old text. The tickets spec requires the replacement to be visible with the write.

### Tests

Default tests do not call Ollama. Unit tests cover all 25 status pairs. Integration and API tests use Testcontainers `pgvector/pgvector:pg16` and a fake embedding client. API tests cover one allowed move and one forbidden move, plus the HTTP cases in the tickets spec.

H2 cannot store `vector(768)`. Repeating all 25 pairs over HTTP would retest one mapping.

### Query binding

A non-integer `page` or `size` returns `400` with the shared error body and `fieldErrors` naming that parameter. This is an assumption. The tickets spec now includes the `page` case so the API test has an oracle.

### Schema startup

Spring runs a `schema.sql` for the ticket tables, the sequence, and the vector table. No migration library is added.

Flyway would be a new dependency for a schema that is created once on an empty database.

## Risks / Trade-offs

- [Embed call holds the database transaction] → The default tests use a fake embedder. A hung Ollama call fails the request with `503` and rolls back, as `spec/rag-ingestion.md` already requires.
- [Only two status pairs are checked over HTTP] → The domain tests cover the other 23 pairs. The service has one branch from that decision to the response.
- [Server-rendered pages cannot be exercised by JUnit] → API tests cover the field messages. Browser checks stay manual for this change.
- [Sequence is new relative to `spec/data-model.md`] → The apply tasks update that file before the schema is written.

## Migration Plan

1. Start the existing Compose Postgres. The database name is `tickets`. The password comes from `DB_PASSWORD`.
2. Start the app. `schema.sql` creates tables only when they are absent.
3. Rollback is to stop the app. The Compose volume keeps the data. Dropping `pgdata` returns to an empty database and the next ticket is `TKT-1001`.

## Open Questions

None. Non-integer paging is decided above. Backfill reporting stays out of scope.
