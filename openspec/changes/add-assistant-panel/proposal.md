# Proposal

## Why

The assistant is specified as a panel on the ticket screens, and no browser UI calls `POST /api/ai/ask`. A separate page would hide the ticket the answer cites.

## What Changes

- Add a side panel, not a `/assistant` page. It is mounted from the frontend root layout, so it sits beside `/`, `/tickets/new`, `/tickets/{id}`, and `/tickets/{id}/edit`.
- The panel has a question box and an Ask button. Ask calls `POST /api/ai/ask` and does not send ticket fields. The question is not copied from the open ticket.
- A grounded answer shows `answer` verbatim, then a Sources list in response order. Each source shows `ticketId`, `title`, and `similarity`. The ticket key links to `/tickets/{ticketId}`.
- When `noRelevantTickets` is true, show the server answer, which is exactly `No relevant tickets found.`, as a distinct no-match message. Do not show an empty answer and do not show sources. It is not an error.
- Show a loading indicator and disable Ask while the request runs. A blank question shows `must not be blank` and does not call the API. A failure shows an error in the panel and leaves the previous answer in place.

## Capabilities

### New Capabilities

- `assistant-panel`: Ask a question from the ticket screens and show the answer, its sources, a no-match message, or an error.

### Modified Capabilities

## Impact

- Depends on `ask` from `add-frontend-api-client`. That client is not implemented, and `frontend/` does not exist.
- `add-ticket-list-ui` and `add-ticket-detail-ui` leave the panel out. This change adds it through the layout, so those pages do not each grow an ask form.
- `spec/ui-flow.md` already chooses the panel. This change adds the loading indicator and the distinct no-match message to that section.
- Asking does not create or edit a ticket.
