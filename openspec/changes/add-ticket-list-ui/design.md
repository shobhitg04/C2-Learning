# Design

## Context

`spec/ui-flow.md` describes the list at `/tickets` with a category column. The request puts the page at `/` and drops category. See proposal.md. `listTickets` is specified in `add-frontend-api-client` and is not implemented. `frontend/` does not exist.

## Goals / Non-Goals

**Goals:**

- Render the list at `frontend/src/app/page.tsx`.
- Load tickets through `listTickets`, with loading, empty, and error states.

**Non-Goals:**

- Detail, create, edit, and the assistant panel.
- Calling `fetch` from the page instead of the API client.

## Decisions

### Client page at `/`

The page is a client component. It calls `listTickets({ page, size: 20, keyword, status })`. Omitted filters are left out of the query. The status dropdown is `All`, `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, and `CANCELLED`.

The table headers are Key, Title, Status, Priority, and Assignee. Title renders `description`. Assignee renders `Unassigned` when null. The key links to `/tickets/{id}`. A Create ticket link goes to `/tickets/new`.

Search resets `page` to `0`. Previous and Next keep the last submitted keyword and status. The banner is the `ApiError` message, or `The server could not be reached.` for any other failure.

If `frontend/src/lib/api.ts` is missing, stop and apply `add-frontend-api-client` first. This change adds `next`, `react`, and `react-dom` only when that package does not already depend on them, plus the App Router page.

Alternative: keep the list at `/tickets`. Rejected because the request chose `app/page.tsx`.

### Tests render the three states

Use the test runner added with the frontend package. Mock `listTickets`. One test expects the first request to use page `0` and size `20`. One expects Search to send the keyword and status. One expects `No tickets match.` for an empty page, and a banner instead of that sentence when the client throws.

## Risks / Trade-offs

- [Title shows the description because the ticket has no title field] → The column stays Title. A later title field can replace the cell without renaming the column.
- [Row and create links go to pages this change does not build] → The list still matches the navigation in `spec/ui-flow.md`.

## Migration Plan

1. Add the page and update the Ticket list section of `spec/ui-flow.md`.
2. Roll back by removing `frontend/src/app/page.tsx`. The API is unchanged.

## Open Questions

None. The list is `/`, and Title is the description.
