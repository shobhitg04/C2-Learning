# Tasks

## 1. Browser client and CORS

- [x] 1.1 Add `frontend/` TypeScript tooling and `frontend/src/lib/api.ts` with `createTicket`, `listTickets`, `getTicket`, `updateTicket`, `addComment`, and `ask`. Read the base URL from `NEXT_PUBLIC_API_URL` and put the current server origin only in an example env file. Verify a mocked `201` create returns the ticket id, and a list call sends `page`, `size`, `keyword`, and `status`.
- [x] 1.2 Throw `ApiError` with `status`, `message`, and `fieldErrors` when the response is not ok. Use message `The server could not be reached.` when the body is not the shared error shape, the network fails, or `NEXT_PUBLIC_API_URL` is missing. Verify a `400` keeps the description field error, a `503` keeps `Ticket could not be indexed` and an empty `fieldErrors`, and a missing base URL does not call `fetch`.
- [x] 1.3 Allow CORS on `/api/**` from `http://localhost:3000` only, for `GET`, `POST`, `PATCH`, and `OPTIONS`. Add one sentence to `spec/architecture.md` that the browser client uses that origin. Verify a preflight from `http://localhost:3000` is allowed and a preflight from `http://localhost:3001` is not.

## Workflow follow-up

- Archive with `/opsx-archive` after review.
