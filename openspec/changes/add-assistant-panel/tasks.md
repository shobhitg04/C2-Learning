# Tasks

## 1. Assistant panel

- [x] 1.1 Confirm `ask` exists in `frontend/src/lib/api.ts`. If it does not, stop and apply `add-frontend-api-client` first. Add `AssistantPanel` and mount it from `frontend/src/app/layout.tsx`. A question is sent alone. A blank question shows `must not be blank` and does not call `ask`. Verify the request body for `Why do quarterly invoice exports fail?` is only that question.
- [x] 1.2 Show a grounded `answer` verbatim, then sources in response order with `ticketId`, `title`, and `similarity`. The ticket key links to `/tickets/{ticketId}`. When `noRelevantTickets` is true, show the server answer as a distinct no-match message, with no sources and no error. Verify a source for `TKT-1001` links to `/tickets/TKT-1001`, and `No relevant tickets found.` is not rendered as an empty answer or as an error.
- [x] 1.3 Show `Asking...` and disable Ask while the request runs. Show `Question could not be answered` for a `503`, and `The server could not be reached.` for a network failure. Leave the previous answer in place. Add those two sentences to the Assistant panel section of `spec/ui-flow.md`. Verify a failed second ask still shows the first answer.

## Workflow follow-up

- Archive with `/opsx-archive` after review.
