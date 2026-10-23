# Proposal

## Why

Ticket and comment rows are written with JDBC against a schema that has no title and no timestamps, and priority stops at `HIGH`. The app needs JPA entities and Spring Data repositories so those fields, a sequence-backed public id, and a test database profile are part of the ticket model.

## What Changes

- Replace the JDBC ticket and comment mapping with JPA entities `Ticket` and `Comment`, a `TicketStatus` enum (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`), and Spring Data repositories.
- Keep public ids as `TKT-` plus `ticket_id_seq`, starting at 1001. An empty database still returns `TKT-1001` for the first ticket.
- Persist `title`, `description`, `priority`, `assignee`, `category`, `resolutionNotes`, `createdAt`, and `updatedAt` on the ticket. Store resolution notes on the ticket row. Drop the separate `ticket_resolution` table.
- **BREAKING:** Create requires a non-blank `title`. A missing or blank title returns `400` with `fieldErrors` field `title` and message `must not be blank`.
- **BREAKING:** Priority accepts `LOW`, `MEDIUM`, `HIGH`, and `CRITICAL` after trim and uppercase. Any other value returns `400` with message `must be LOW, MEDIUM, HIGH, or CRITICAL`. `CRITICAL` is a new stored value. The previous message named only `HIGH`, `MEDIUM`, and `LOW`.
- Read and list responses include `title`, `createdAt`, and `updatedAt`. Keyword search also matches `title`.
- Point the PostgreSQL datasource at `DB_URL`, `DB_USER`, and `DB_PASSWORD`. The password has no default. Use `spring.jpa.hibernate.ddl-auto=update` for now.
- Add `application-test.yml` that uses H2. Existing API and vector tests stay on Testcontainers `pgvector/pgvector:pg16`.

Assumption: `DB_USER` keeps the default `tickets`. `DB_URL` and `DB_PASSWORD` have no default.

## Capabilities

### New Capabilities

### Modified Capabilities

- `tickets`: Create requires `title`. Priority includes `CRITICAL` and the unknown-priority message changes. Read and list expose `title`, `createdAt`, and `updatedAt`, and keyword search matches `title`.

## Impact

- `backend/` today maps tickets with `JdbcTemplate` in `tickets.repository.TicketRepository`, domain records `Ticket` and `Comment`, and enum `Status`. Callers are `TicketService`, `TicketIndexer`, and `TicketPageController`.
- `spec/data-model.md` still checks priority as `HIGH|MEDIUM|LOW`, keeps resolution notes in `ticket_resolution`, and has no `title`, `created_at`, or `updated_at`. That document is updated with this change. `vector_store` stays as it is.
- `schema.sql` currently creates `ticket`, `comment`, and `ticket_resolution`. Those three leave `schema.sql` so Hibernate owns them. The vector extension, `vector_store`, and `ticket_id_seq` stay.
- New dependencies: `spring-boot-starter-data-jpa`, and `h2` at test scope.
- The create and edit screens gain a title field. The state machine, comment API, re-index rules, and the assistant ask API do not change.
- `spec/test-strategy.md` says the default suite does not use H2. This change does not move that suite. H2 is only the `test` profile used by the new repository test.
