# Tasks

## 1. List page

- [ ] 1.1 Confirm `frontend/src/lib/api.ts` exports `listTickets`. If it does not, stop and apply `add-frontend-api-client` first. Add `frontend/src/app/page.tsx` at `/` with the Key, Title, Status, Priority, and Assignee table. Title is the description. A null assignee is `Unassigned`. The first request uses page `0` and size `20`. Verify a mocked first load renders those five cells and does not render category.
- [ ] 1.2 Add the keyword box, the status dropdown, Search, and Previous/Next. Search runs only from the button, omits a blank keyword, omits `All`, and resets to page `0`. Previous is disabled on page `0`. Next is disabled when no further page exists. The label is `Page {page + 1}`. Verify Search sends `keyword=export` and `status=OPEN`.
- [ ] 1.3 Show `Loading tickets.` while the request runs, `No tickets match.` when `content` is empty, and a banner with the error message when `listTickets` fails. A failure must not show the empty sentence. Rewrite the Ticket list section of `spec/ui-flow.md` for the `/` route, these columns, and the loading text, and keep the search-on-submit trade-off. Verify the empty response and a thrown error produce those two different messages.

## Workflow follow-up

- Archive with `/opsx-archive` after review.
