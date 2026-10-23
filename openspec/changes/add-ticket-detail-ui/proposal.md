# Proposal

## Why

The create, detail, and edit screens are how a person files a ticket, reads it, and changes it. None of those browser pages exist.

## What Changes

- Add a create form at `/tickets/new` with title, description, priority, and assignee. It does not send category, status, or resolution notes.
- Add the detail page at `/tickets/{id}` in `frontend/src/app/tickets/[id]/page.tsx`. It shows the ticket, the comments, an add-comment form, and a status control.
- The status control lists only the transitions `spec/state-machine.md` marks Allowed from the loaded status. It does not list the current status or a forbidden target.
- Add an edit form at `/tickets/{id}/edit` for title, description, priority, and assignee. It does not edit category, status, or resolution notes.
- Show each `fieldErrors` message under the matching input. A `409` shows the backend `message` in a visible alert.

Assumption: an empty assignee on create is omitted. An emptied assignee on edit sends `null`. Priority is `HIGH`, `MEDIUM`, or `LOW`. A blank required field shows `must not be blank` and does not call the API. The assistant panel is not on these pages.

## Capabilities

### New Capabilities

- `ticket-detail-ui`: Create a ticket, read it, comment on it, change its status, and edit its title, description, priority, and assignee in the browser.

### Modified Capabilities

## Impact

- The current create API requires category and has no title. `PATCH /api/tickets/{id}` updates status, assignee, and resolution notes. It does not store title, description, or priority.
- `add-ticket-service` still requires category on create, so applying that change does not by itself make this form succeed.
- The status control sends only `status`. It uses `PATCH /api/tickets/{id}/status` when that route exists, and otherwise the general PATCH.
- Depends on `createTicket`, `getTicket`, `updateTicket`, and `addComment` from `add-frontend-api-client`.
- `spec/ui-flow.md` still describes the old create and edit fields. This change rewrites those sections.
