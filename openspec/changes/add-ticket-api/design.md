# Design

## Context

`TicketController` already maps create, list, get, patch, and comments. `ApiErrors` is a package-private advice in the same file and handles only `ApiException`. `TicketService.patch` catches a forbidden pair and throws `ApiException` 409. See proposal.md for why the status route and the advice mapping are split out. The scenarios are in `specs/tickets/spec.md`.

`InvalidTransitionException` comes from `add-ticket-state-machine`. This change does not define the allowed pairs.

## Goals / Non-Goals

**Goals:**

- One advice class builds the shared error body.
- `PATCH /api/tickets/{id}/status` returns `200`, `400`, `404`, or `409` as the delta specifies.

**Non-Goals:**

- Removing `status` from the general patch. `add-ticket-service` does that.
- Bean Validation lengths, keyword narrowing, or a new validation starter.
- Changing the status matrix or the `503` indexing body.

## Decisions

### One advice class

`tickets.controller.ApiErrors` is the only `@RestControllerAdvice`. It maps:

- validation failures to `400` `Bad Request`, message `Validation failed`, and one `fieldErrors` entry per field
- an unknown ticket to `404` `Not Found`, message `Ticket {id} not found`, `fieldErrors` `[]`
- `InvalidTransitionException` to `409` `Conflict`, `fieldErrors` `[]`, and the exception message `Status cannot change from {current} to {requested}`

`ApiException` stays for `400`, `404`, and `503`. A `MethodArgumentNotValidException`, if Bean Validation is already on the classpath, uses the same `400` body. The advice does not add the validation starter.

Alternative: keep building `409` inside `TicketService`. Rejected because the request maps `InvalidTransitionException` in the advice.

### The status route calls the state machine

`PATCH /api/tickets/{id}/status` reads `{ "status": "IN_PROGRESS" }`. A value outside the five statuses returns `400` and does not call the state machine. A known ticket calls `TicketStateMachine.require`. The service does not catch that exception. An unknown id throws the existing not-found error before the transition check.

The general patch is left as it is in this change. Both routes can change status until `add-ticket-service` rejects `status` on the general patch.

Alternative: make this route the only status write now. Rejected because that break is already specified on the service change, and doing it here would contradict that change's ownership.

## Risks / Trade-offs

- [`add-ticket-service` also adds this route and a Bean Validation mapping] → This change adds the route and the `InvalidTransitionException` handler. It does not add length rules. Whichever change applies second keeps one handler and one route.
- [`InvalidTransitionException` is not in the tree yet] → Stop and apply `add-ticket-state-machine` before editing the advice. Do not invent a second exception type.
- [A forbidden general patch still throws `ApiException` until the service lets the domain exception through] → The status route must let `InvalidTransitionException` reach the advice. The general patch may keep its current `409` until the service change.

## Migration Plan

1. Move the advice and add the status route.
2. Point the status write at `TicketStateMachine.require`.
3. Roll back by deleting the route and the exception handler. Stored statuses do not change.

## Open Questions

None. The `409` message is `Status cannot change from {current} to {requested}`.
