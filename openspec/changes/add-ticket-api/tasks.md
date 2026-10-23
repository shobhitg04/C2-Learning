# Tasks

## 1. Error advice

- [ ] 1.1 Confirm `tickets.domain.InvalidTransitionException` exists. If it does not, stop and apply `add-ticket-state-machine` first. Verify `./mvnw -q -DskipTests compile` from `backend/` exits 0.
- [ ] 1.2 Make `tickets.controller.ApiErrors` the only `@RestControllerAdvice` for the ticket API. Map validation failures to `400` with one `fieldErrors` entry per field, unknown tickets to `404` with message `Ticket {id} not found` and empty `fieldErrors`, and `InvalidTransitionException` to `409` with message `Status cannot change from {current} to {requested}` and empty `fieldErrors`. Keep the `503` indexing body. Verify an API test that a blank description returns `400` with the five error fields and a `description` entry, and that `GET /api/tickets/TKT-9999` returns `404` with that message and empty `fieldErrors`.

## 2. Status route

- [ ] 2.1 Add `PATCH /api/tickets/{id}/status`. A status outside the five names returns `400` and does not call the state machine. A known ticket calls `TicketStateMachine.require` and lets `InvalidTransitionException` reach the advice. An unknown id returns `404` before the transition check. Do not remove `status` from the general patch. Verify `OPEN` to `IN_PROGRESS` returns `200`, `OPEN` to `OPEN` returns `409` with message `Status cannot change from OPEN to OPEN` and an unchanged status, `status` `DONE` returns `400`, and an unknown id returns `404`.
- [ ] 2.2 Add the status route to `spec/api-contract.md`, including the `409` message and the `400` for a value that is not a status. Verify that file contains `PATCH /api/tickets/{id}/status` and `Status cannot change from OPEN to OPEN`.

## Workflow follow-up

- Apply `add-ticket-state-machine` before this change so the exception type exists.
- Leave the general-patch status rejection to `add-ticket-service`.
- Archive with `/opsx-archive` after review.
