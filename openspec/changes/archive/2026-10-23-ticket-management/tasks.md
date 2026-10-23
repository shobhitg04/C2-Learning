# Tasks

## 1. Application skeleton

- [x] 1.1 Create `backend/` as a Java 21 Spring Boot 3 app with packages `tickets.controller`, `tickets.service`, `tickets.repository`, `tickets.domain`, and `tickets.ai`, and verify `./mvnw -q -DskipTests compile` from `backend/` exits 0
- [x] 1.2 Point the app at Compose Postgres database `tickets` using `DB_PASSWORD` from the environment, and verify the app context starts against Testcontainers `pgvector/pgvector:pg16`

## 2. Status rules and schema

- [x] 2.1 Add `ticket_id_seq` starting at 1001 to `spec/data-model.md`, and verify the file names the sequence and the `TKT-` format
- [x] 2.2 Add `schema.sql` for ticket, comment, ticket_resolution, the sequence, and `vector_store`, and verify the integration test can create those objects on a fresh database
- [x] 2.3 Implement the 25-pair status decision in `tickets.domain`, and verify one unit test passes for each allowed pair and each forbidden pair in `spec/state-machine.md`

## 3. Ticket persistence

- [x] 3.1 Implement ticket, comment, and resolution repositories, and verify an integration test stores a ticket, orders comments by `created_at` then id, and keeps at most one resolution row
- [x] 3.2 Assign `TKT-1001` from `ticket_id_seq` on an empty database, and verify that integration test

## 4. Ticket HTTP API

- [x] 4.1 Implement create, get, list, patch, and comment endpoints with the shared error body, and verify API tests for `201`, blank description `400`, priority `high` stored as `HIGH`, unknown priority `400`, unknown id `404`, malformed id `400`, and empty patch `400`
- [x] 4.2 Implement keyword and status filters and paging, and verify API tests for `page=0&size=1`, keyword AND status, a page past the end, and a non-integer `page`

## 5. Re-index on write

- [x] 5.1 Implement section chunking, including one chunk for a description of 500 words and two chunks with `partIndex` 1 and 2 for 501 words, and verify those unit tests
- [x] 5.2 Delete then insert that ticket's chunks in the same transaction as a successful write, using a fake 768-dimension embedder in tests, and verify an integration test that a second save does not leave the previous resolution text searchable
- [x] 5.3 Skip re-index on validation failure and on a forbidden transition, and verify the forbidden API test returns `409` with message `Status cannot change from OPEN to OPEN` and the same chunk ids

## 6. Allowed transition over HTTP

- [x] 6.1 On an allowed patch, store the new status and re-index once, and verify `OPEN` to `IN_PROGRESS` returns `200` and `RESOLVED` to `CLOSED` keeps resolution notes

## 7. Ticket screens

- [x] 7.1 Add list, detail, create, and edit pages that call the ticket API, and verify a slice test that `GET /tickets` returns the keyword field and `GET /tickets/new` returns the description field
- [x] 7.2 Block a blank description on the create page before the request, and show `fieldErrors` messages under the matching fields, and verify the page script contains the check and the server message `must not be blank` is what a failed create displays

## Workflow follow-up

- Review the tickets spec and the API tests before archive.
- Archive this change only after implementation is accepted.
