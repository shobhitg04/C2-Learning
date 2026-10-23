# Proposal

## Why

Ticket HTTP errors are built inside `TicketController` from `ApiException`, and status changes share `PATCH /api/tickets/{id}` with other fields. The API needs one advice class for the shared error body and a dedicated status route that turns `InvalidTransitionException` into `409`.

## What Changes

- Keep `TicketController` as the ticket HTTP adapter and add `PATCH /api/tickets/{id}/status`.
- Use one `@RestControllerAdvice` for ticket API errors. Validation failures return `400` with one `fieldErrors` entry per invalid field. An unknown ticket returns `404`, message `Ticket {id} not found`, and `fieldErrors` `[]`.
- `InvalidTransitionException` returns `409`, `error` `Conflict`, `fieldErrors` `[]`, and message `Status cannot change from {current} to {requested}`. The row and its chunks stay unchanged.
- Every `400`, `404`, and `409` body has only `timestamp`, `status`, `error`, `message`, and `fieldErrors`.
- A status value outside the five statuses stays `400`, not `409`.

Assumption: this change adds the status route. It does not remove `status` from the general patch. `add-ticket-service` is the change that rejects `status` on that patch.

## Capabilities

### New Capabilities

### Modified Capabilities

- `tickets`: Status can be changed through `PATCH /api/tickets/{id}/status`. The shared error body covers validation `400`, unknown-ticket `404`, and invalid-transition `409`.

## Impact

- `backend/src/main/java/tickets/controller/TicketController.java` already serves create, list, get, patch, and comments. `ApiErrors` in that file handles only `ApiException`.
- `spec/api-contract.md` has no status route. The general patch still accepts `status`. This change adds the route to that document and leaves the general patch as it is.
- Depends on `add-ticket-state-machine` for `InvalidTransitionException`. `add-ticket-service` also specifies this route and the advice mapping for Bean Validation. This change does not add the validation starter, title lengths, or the narrower keyword search.
- The list stays paginated. The `503` indexing error stays on the same advice.
