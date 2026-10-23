# Proposal

## Why

The ticket list is specified and there is no browser page for it. `frontend/` does not exist yet, and `add-frontend-api-client` stops before any screen.

## What Changes

- Add the list at `/` in `frontend/src/app/page.tsx`.
- Show a table with Key, Title, Status, Priority, and Assignee. Key is the ticket id. Title is the description. A null assignee is `Unassigned`. Category is not a column.
- Add a keyword box, a status dropdown, and a Search button. Search runs on submit, not on each keystroke. Page size stays `20`. Previous and Next change the page.
- Show `Loading tickets.` while the request runs, `No tickets match.` when the page is empty, and a banner with the error message when the API fails. A failure does not use the empty state.

Assumption: a row links to `/tickets/{id}`, and the page links to `/tickets/new`. Those screens are not part of this change. The assistant panel is not on this page.

## Capabilities

### New Capabilities

- `ticket-list-ui`: Show one page of tickets at `/`, with search, status, pagination, loading, empty, and error states.

### Modified Capabilities

## Impact

- Depends on `listTickets` from `add-frontend-api-client`. That change is not applied, and `frontend/` is absent.
- `spec/ui-flow.md` still says the list is `/tickets` and that a row includes `category`. This change rewrites that section.
- The Thymeleaf list stays.
