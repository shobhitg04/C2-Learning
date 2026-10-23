# Tasks

## 1. Create, detail, and edit

- [ ] 1.1 Confirm `createTicket`, `getTicket`, `updateTicket`, and `addComment` exist. If they do not, stop and apply `add-frontend-api-client` first. Then confirm create accepts title, description, priority, and assignee without category, and patch stores title, description, and priority. If either confirmation fails, stop and report that gap. Do not send a hidden category. Add `/tickets/new` for title, description, priority, and assignee. Verify a completed create posts those fields, omits assignee when it is empty, and opens `/tickets/{id}`.
- [ ] 1.2 Add `frontend/src/app/tickets/[id]/page.tsx` with the ticket fields, the comments, and the add-comment form. A blank comment shows `must not be blank` and does not call the API. The status control lists only Allowed targets from `spec/state-machine.md` for the loaded status and sends `{ "status" }` only, using `PATCH /api/tickets/{id}/status` when that route exists. A `409` shows the backend message in a visible alert. Verify an `OPEN` ticket offers `IN_PROGRESS` and not `OPEN`, and a `409` message `Status cannot change from OPEN to OPEN` is in the alert.
- [ ] 1.3 Add `/tickets/{id}/edit` for title, description, priority, and assignee. Show each `fieldErrors` message under the matching input. A blank title or description shows `must not be blank` and does not call the API. Rewrite the create, detail, and edit sections of `spec/ui-flow.md` for these fields and for the status control on the detail page. Verify a description `fieldErrors` message is under that input and is not the only copy in the alert.

## Workflow follow-up

- Archive with `/opsx-archive` after review.
