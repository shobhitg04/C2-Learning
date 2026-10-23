# Design

## Context

`spec/ui-flow.md` puts category on create and status plus resolution notes on edit. The request replaces those fields. See proposal.md. `GET /api/tickets/{id}` returns `description`, not `title`. `PATCH` accepts `status`, `assignee`, and `resolutionNotes`. `spec/state-machine.md` is the allowed-pair matrix the screen must follow. `add-frontend-api-client` names `createTicket`, `getTicket`, `updateTicket`, and `addComment`, and it is not applied.

## Goals / Non-Goals

**Goals:**

- Create, detail, comment, status, and edit screens that match the chosen fields.
- Put `fieldErrors` under the inputs and a `409` message in an alert.

**Non-Goals:**

- The assistant panel.
- Editing category or resolution notes.
- Replacing the Thymeleaf pages.

## Decisions

### Three routes

`frontend/src/app/tickets/new/page.tsx` is create. `frontend/src/app/tickets/[id]/page.tsx` is detail. `frontend/src/app/tickets/[id]/edit/page.tsx` is edit. All three are client components and call the API client. The detail page links to the edit route.

The status control is a separate action on the detail page. Its options are the Allowed column for the loaded row in `spec/state-machine.md`. The current status is not an option. The request body is `{ "status" }` only. Call `PATCH /api/tickets/{id}/status` when that route exists. Otherwise call `updateTicket` with only `status`.

A `409` renders in an element with `role="alert"`. The ticket text stays as loaded. `fieldErrors` render under the matching input and are not copied into that alert.

If `createTicket` is missing, stop and apply `add-frontend-api-client` first. If create still requires category, or the ticket has no title, or patch cannot store title, description, and priority, stop and report that gap. Do not send a hidden category.

Alternative: keep status on the edit form. Rejected because the request puts it on the detail page.

### Client checks before the request

Blank title, description, priority, or comment shows `must not be blank` and does not call the API. Whitespace in assignee shows the same message. Create omits an empty assignee. Edit sends `null` when the user clears assignee. Priority is a select of `HIGH`, `MEDIUM`, and `LOW`.

## Risks / Trade-offs

- [Create cannot succeed on the current API] → The API requires category and has no title. This change does not invent those columns. The task stops until a backend change accepts this body.
- [The status list can be stale] → The alert shows the `409` message when the ticket changed after the page loaded.
- [`add-ticket-service` still requires category] → Applying it does not remove this gap.

## Migration Plan

1. Add the three pages and rewrite the create, detail, and edit sections of `spec/ui-flow.md`.
2. Roll back by removing those pages. Stored tickets are unchanged.

## Open Questions

None. Title is a real field once the API has it. Until then, implementation stops.
