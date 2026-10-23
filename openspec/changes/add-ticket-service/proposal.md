# Proposal

## Why

`TicketService` already creates, lists, and patches tickets, but status rides on the general update, keyword search looks past title and description, and field lengths are not checked. The service needs request records, Bean Validation, and a status change that goes through `TicketStateMachine`.

## What Changes

- Add request and response records for create, list, get, update, comment, and status change. The controller validates them and `TicketService` performs the writes.
- **BREAKING:** `PATCH /api/tickets/{id}` updates `title`, `description`, `priority`, `assignee`, and `resolutionNotes` only. A body that includes `status` returns `400`, field `status`, message `must not be set`, and leaves the stored status unchanged.
- Status changes use `PATCH /api/tickets/{id}/status` and `TicketStateMachine`. Allowed pairs return `200`. Forbidden pairs return `409` with no row change and no re-index.
- **BREAKING:** `keyword` is a case-insensitive substring of `title` and `description` only. It no longer matches `id`, `category`, comment body, or resolution notes.
- **BREAKING:** `title` must be 5 to 120 characters after trim. A missing, blank, or other length returns `400`, field `title`, message `must be between 5 and 120 characters`.
- `description` stays required. Blank stays `must not be blank`. Longer than 5000 characters returns `400`, message `must be at most 5000 characters`.
- Comment `body` stays required. Blank stays `must not be blank`. Longer than 2000 characters returns `400`, message `must be at most 2000 characters`.
- Create still requires `category` and rejects `status`. Priority stays the domain enum. List paging and the `status` filter stay as they are.

Assumption: resolution notes stay on the general update. This request does not remove them. `category` is set on create and is not patchable. Comment JSON stays `body`.

## Capabilities

### New Capabilities

### Modified Capabilities

- `tickets`: Create and update enforce the title, description, and comment lengths. Keyword search matches title and description only. Status changes move to their own endpoint.

## Impact

- `TicketService`, `TicketController`, `TicketPageController`, and the API tests. The edit screen must send status to the new path.
- `spec/api-contract.md` still documents status, assignee, and resolution notes on one `PATCH`, and a wider keyword. That file is updated with this change.
- Depends on `add-ticket-persistence` for a stored `title`, and on `add-ticket-state-machine` for `TicketStateMachine`. Those changes also edit the `tickets` spec. Archive them first so this delta is reconciled with the updated main spec.
- New dependency: `spring-boot-starter-validation`.
- The assistant ask API is unchanged.
