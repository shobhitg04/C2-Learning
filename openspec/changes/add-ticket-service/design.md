# Design

## Context

`TicketController` parses JSON by hand and `TicketService` validates with its own checks. `PATCH /api/tickets/{id}` accepts `status`. Keyword search matches id, description, category, comments, and resolution notes. There is no `title` length rule. See proposal.md for the new contract and `specs/tickets/spec.md` for the scenarios.

`add-ticket-persistence` adds the stored title. `add-ticket-state-machine` adds `TicketStateMachine`. This change does not re-implement either one.

## Goals / Non-Goals

**Goals:**

- Request records with Bean Validation and the shared error body.
- A status endpoint that calls `TicketStateMachine`, and a general update that refuses `status`.
- Keyword search limited to title and description.

**Non-Goals:**

- Changing the priority set, the status matrix, paging defaults, or the assistant API.
- Removing resolution notes from the general update.
- Making `category` patchable.

## Decisions

### Records at the controller, rules in annotations

Request and response types are records in `tickets.controller`. Each request record trims text in its compact constructor, and uppercases `priority`, before validation runs. Annotations carry the spec messages: title `@Size(min = 5, max = 120)`, description `@NotBlank` and `@Size(max = 5000)`, comment `body` `@NotBlank` and `@Size(max = 2000)`. `status` on create and on the general update is `@Null` with message `must not be set`.

The existing `@RestControllerAdvice` maps `MethodArgumentNotValidException` to the shared body. One `fieldErrors` entry per field. The controller calls the service with the validated values. The service keeps `@Transactional` on writes and does not repeat those length checks.

Alternative: keep the hand-written checks in `TicketService`. Rejected because the request asks for Bean Validation on the records.

### Status has its own route

`PATCH /api/tickets/{id}/status` takes `{ "status": "IN_PROGRESS" }`. The service calls `TicketStateMachine.require`. `InvalidTransitionException` becomes the current `409`. A value outside the five statuses is `400`, not `409`.

The general update record includes `status` only so a present value fails `@Null`. Omitting it leaves the stored status alone.

Alternative: ignore unknown `status` because the current contract ignores unknown JSON. Rejected because a silent drop would still let a client believe the status changed.

### Keyword query

The list query matches `keyword` with case-insensitive `LIKE` against `title` and `description` only. `%` and `_` stay escaped. The `status` filter and paging rules stay.

### Priority values come from the domain

The priority constraint reads the domain priority values already in the app. It does not declare a second set. Today those values are `HIGH`, `MEDIUM`, and `LOW`. After `add-ticket-persistence`, the same constraint accepts `CRITICAL`.

### Screens

The create page blocks a title shorter than 5 characters before the request and shows `must be between 5 and 120 characters`. The edit page sends a changed status with `PATCH /api/tickets/{id}/status` and sends title, description, priority, assignee, and resolution notes without `status`.

## Risks / Trade-offs

- [Clients that patch `status` on `/api/tickets/{id}`, or search comments and ids, now fail or get fewer rows] → The `400` names `status`. Keyword behavior is documented in `spec/api-contract.md`.
- [This delta and the two open changes all modify `tickets`] → Apply persistence and the state machine first. If `title` or `TicketStateMachine` is missing, stop this change rather than inventing a second model.
- [Two annotations on one field can emit two errors] → The advice keeps a single message per field, preferring `must not be blank` over the length message when the trimmed value is empty.

## Migration Plan

1. Add the validation starter, records, and the status route.
2. Narrow the keyword query and move the edit screen's status submit.
3. Roll back by restoring hand-written validation and status on the general patch. Stored rows do not change shape in this change.

## Open Questions

None. Resolution notes stay on the general update. Comment JSON stays `body`.
