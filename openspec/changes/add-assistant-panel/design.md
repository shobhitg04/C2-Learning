# Design

## Context

`spec/ui-flow.md` puts the assistant on every ticket screen and rejects a separate `/assistant` page. `POST /api/ai/ask` returns `answer`, `sources`, and `noRelevantTickets`. See proposal.md. `ask` is named in `add-frontend-api-client` and is not implemented. `frontend/` does not exist. The list change uses `/`, and the detail change uses `/tickets/new`, `/tickets/{id}`, and `/tickets/{id}/edit`.

## Goals / Non-Goals

**Goals:**

- One side panel that asks a question and shows the answer, sources, a no-match message, loading, or an error.
- Keep a no-match result distinct from an empty answer and from an error.

**Non-Goals:**

- A standalone assistant route.
- Changing tickets, comments, or status from the panel.

## Decisions

### Panel in the root layout

`frontend/src/components/AssistantPanel.tsx` is a client component. `frontend/src/app/layout.tsx` renders it beside the page. One layout covers `/` and the ticket routes. If the layout file is missing, this change creates it. If `ask` is missing, stop and apply `add-frontend-api-client` first.

Ask is disabled while the request runs, and the panel shows `Asking...`. A success with `noRelevantTickets` false replaces the answer and the source list. A success with `noRelevantTickets` true clears the source list and shows the server `answer` in a no-match region, not in the error region. A thrown `ApiError` shows `message` in the error region. Any other failure shows `The server could not be reached.` The previous answer and no-match message stay on screen.

A blank trimmed question sets `must not be blank` on the question field and does not call `ask`.

Alternative: a route at `/assistant`. Rejected because the source link would be the only way back to the ticket.

### Sources keep the contract fields

Each source row shows `ticketId`, `title`, and `similarity` as returned. The key is a link to `/tickets/{ticketId}`. Order is the response order.

## Risks / Trade-offs

- [The panel uses space on every screen, including create] → That is the placement `spec/ui-flow.md` already chose.
- [The list and detail changes are not applied] → The layout shows the panel as soon as those pages exist. This change does not build those pages.

## Migration Plan

1. Add the panel and the layout, and add the loading and no-match sentences to the Assistant panel section of `spec/ui-flow.md`.
2. Roll back by removing the panel from the layout. Tickets are unchanged.

## Open Questions

None. The no-match text is the server answer, including the period.
