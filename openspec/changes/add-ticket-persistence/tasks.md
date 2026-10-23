# Tasks

## 1. Persistence setup

- [ ] 1.1 Add `spring-boot-starter-data-jpa` and test-scoped `h2` in `backend/pom.xml`. Point `application.yml` at `${DB_URL}`, `${DB_USER:tickets}`, and `${DB_PASSWORD}` with no password default, set `spring.jpa.hibernate.ddl-auto` to `update`, and set `spring.jpa.open-in-view` to `false`. Verify `./mvnw -q -DskipTests compile` from `backend/` exits 0.
- [ ] 1.2 Add `application-test.yml` for the `test` profile using H2 `jdbc:h2:mem:tickets;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`, `ddl-auto: update`, and a sequence-only init script with `ticket_id_seq` starting at 1001. Do not load `schema.sql` in that profile. Verify a `@SpringBootTest` with `@ActiveProfiles("test")` starts and the first `nextval('ticket_id_seq')` is 1001.

## 2. Entities and schema

- [ ] 2.1 Replace `Status` with `TicketStatus` (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`) and add `TicketPriority` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`). Make `Ticket` and `Comment` JPA entities with the ticket fields from the proposal, a nullable `resolutionNotes` column, and `@PrePersist` / `@PreUpdate` timestamps. Generate the text id as `TKT-` plus `nextval('ticket_id_seq')`. Replace `TicketRepository` with Spring Data repositories, loading comments by `createdAt` then id, and keep `JdbcTemplate` for `vector_store` only. Verify the existing 25 status-policy tests pass against `TicketStatus`.
- [ ] 2.2 Remove `ticket`, `comment`, and `ticket_resolution` from `schema.sql`, and keep the vector extension, `vector_store`, and `ticket_id_seq`. Update `spec/data-model.md` so the ticket row has `title`, `created_at`, `updated_at`, and `resolution_notes`, priority includes `CRITICAL`, and resolution notes are no longer a separate table. Keep the trade-off paragraph and state the column decision. Verify `spec/data-model.md` names those columns and `CRITICAL`, and verify the pgvector integration test still finds `ticket_id_seq` starting at 1001 and `vector_store`.
- [ ] 2.3 Verify an H2 `@ActiveProfiles("test")` repository test: the first saved ticket is `TKT-1001` and the next is `TKT-1002`; comments come back by `createdAt` then id; resolution notes live on that one ticket row; `createdAt` equals `updatedAt` on create; a comment leaves both instants unchanged; changing the assignee moves only `updatedAt`.

## 3. Ticket API

- [ ] 3.1 Wire `TicketService`, the indexer, and the controllers through the JPA repositories. Require a non-blank title on create, accept `CRITICAL`, return `title`, `createdAt`, and `updatedAt` on read, and match keyword search against title. Update the existing API tests for the new create body and priority message. Verify API tests for a valid create storing title `Export timeout`, a blank title `400` with message `must not be blank`, priority `critical` stored as `CRITICAL`, unknown priority message `must be LOW, MEDIUM, HIGH, or CRITICAL`, a read that includes `createdAt` and `updatedAt`, and `keyword=payroll` matching a title.
- [ ] 3.2 Update `spec/api-contract.md` and the priority rows in `spec/test-strategy.md` for required title, `CRITICAL`, timestamps, and title keyword search. Verify those files state the blank-title message and the four priority values.

## 4. Ticket screens

- [ ] 4.1 Add a title field to the create screen, show title on the list and detail screens, and block a blank title before the request with `must not be blank`. Update `spec/ui-flow.md` for that field and message. Verify a slice test that `GET /tickets/new` contains the title field and the blank-title check, and that a failed create surfaces `must not be blank` for `title`.

## Workflow follow-up

- Review the tickets delta and the API tests before archive.
- Archive with `/opsx-archive` after implementation is accepted.
