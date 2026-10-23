# Design

## Context

`backend/` is a Spring Boot 3.4.5 / Java 21 app. `TicketRepository` uses `JdbcTemplate`. `Ticket` and `Comment` are records. `Status` is the status enum. `schema.sql` creates `ticket`, `comment`, `ticket_resolution`, `ticket_id_seq`, and `vector_store`. See proposal.md for why this change exists. The behavior deltas are in `specs/tickets/spec.md`.

`vector_store` stays in `schema.sql` and is still written with JDBC. H2 cannot run `CREATE EXTENSION vector`, so the existing API tests stay on Testcontainers `pgvector/pgvector:pg16`.

## Goals / Non-Goals

**Goals:**

- Map ticket and comment rows with JPA and Spring Data, including the public id sequence.
- Let Hibernate update the ticket and comment tables (`ddl-auto=update`).
- Give repository tests an H2 profile that does not load the vector schema.

**Non-Goals:**

- Flyway or Liquibase.
- A JPA entity for `vector_store`.
- Changing the status matrix, comment rules, or re-index rules.
- Moving the existing API tests off Testcontainers.

## Decisions

### JPA entities replace the JDBC mapping

`tickets.domain.Ticket` and `tickets.domain.Comment` become entities. `TicketStatus` replaces `Status` with the same five constants, stored as `STRING`. `TicketPriority` is `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`, also stored as `STRING`. `StatusPolicy` uses `TicketStatus`.

`TicketRepository` becomes `JpaRepository<Ticket, String>`. `CommentRepository` becomes `JpaRepository<Comment, Long>` and loads comments with `createdAt` then `id`. The list and keyword query moves to a Spring Data `@Query` on `TicketRepository`. `JdbcTemplate` remains only for `vector_store`.

Alternative: keep the JDBC repository and add entities beside it. Rejected because both would map the same tables and the same type names.

### Public id from `ticket_id_seq`

The primary key stays the text id `TKT-` plus `nextval('ticket_id_seq')`. A Hibernate `IdentifierGenerator` reads that sequence. `schema.sql` keeps `CREATE SEQUENCE IF NOT EXISTS ticket_id_seq START 1001`. Allocation size is 1, so an empty database still yields `TKT-1001`.

Alternative: a numeric primary key plus a separate display id. Rejected because the API and `vector_store` metadata already use the text id as the key.

### Resolution notes are a nullable column

`ticket.resolution_notes` replaces `ticket_resolution`. Null means no notes and no resolution chunk. The indexer reads the column from the ticket entity.

Alternative: keep the one-row table and map it as a secondary entity. Rejected because the requested ticket fields include `resolutionNotes` on `Ticket`.

### Timestamps are set on the ticket entity

`@PrePersist` sets `createdAt` and `updatedAt` to the same instant. `@PreUpdate` moves only `updatedAt`. Comments are saved through `CommentRepository` with no cascade from `Ticket`, so adding a comment does not mark the ticket dirty.

Alternative: `@UpdateTimestamp` on the ticket while cascading comments. Rejected because a comment insert would change `updatedAt`, which the spec forbids.

### Datasource and test profile

`application.yml` uses `url: ${DB_URL}`, `username: ${DB_USER:tickets}`, and `password: ${DB_PASSWORD}` with no password default. `spring.jpa.hibernate.ddl-auto` is `update`. `spring.jpa.open-in-view` is `false`; the service loads comments inside the transaction before the view renders.

`application-test.yml` sets the `test` profile to H2 (`jdbc:h2:mem:tickets;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`), `ddl-auto: update`, and `spring.sql.init.schema-locations` to a sequence-only script. It does not run `schema.sql`. The new repository test activates `test`. Existing `@SpringBootTest` API tests do not, and they keep overriding the datasource to Testcontainers.

`schema.sql` drops the `ticket`, `comment`, and `ticket_resolution` statements so the old priority check is not reapplied. It still creates the vector extension, `vector_store`, and `ticket_id_seq`.

Alternative: H2 for every test. Rejected because `vector_store` needs pgvector.

### Dependencies

Add `spring-boot-starter-data-jpa`. Add `h2` with test scope. Keep `spring-boot-starter-jdbc` for the indexer.

## Risks / Trade-offs

- [Existing Postgres volume still has the old `ticket` table and `ticket_resolution`] → Recreate the local database before boot. `ddl-auto=update` will not fill `title` for old rows or drop the unused resolution table.
- [Hibernate update does not enforce the old SQL check constraints] → Priority and status are enums. Blank title and description stay in the service validation that already returns `400`.
- [H2 sequence syntax differs from Postgres] → The test script uses `START WITH 1001`, which H2 accepts. Postgres keeps `START 1001` in `schema.sql`.
- [`ddl-auto=update` can alter a shared database on startup] → Accepted for now, as requested. A later change can switch to `validate` once the schema is stable.

## Migration Plan

1. Add the JPA mapping and stop `schema.sql` from creating ticket tables.
2. Recreate the local `tickets` database so Hibernate builds the new columns.
3. Roll back by restoring the JDBC repository and the previous `schema.sql`. No production data migration is in this change.

## Open Questions

None. `title` is required on create and is not patchable. `DB_USER` defaults to `tickets`.
