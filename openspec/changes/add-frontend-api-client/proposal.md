# Proposal

## Why

The ticket and ask APIs exist, and `spec/ui-flow.md` calls them from the browser. There is no `frontend/` package, and the backend allows no browser origin, so a page on port 3000 cannot call the API.

## What Changes

- Add a typed client at `frontend/src/lib/api.ts` for `POST /api/tickets`, `GET /api/tickets`, `GET /api/tickets/{id}`, `PATCH /api/tickets/{id}`, `POST /api/tickets/{id}/comments`, and `POST /api/ai/ask`.
- Read the API host from `NEXT_PUBLIC_API_URL`. The client does not hardcode a host or port.
- When a response is not ok, parse the shared error body and throw `ApiError` with `status`, `message`, and `fieldErrors`.
- Allow CORS from `http://localhost:3000` only.

Assumption: this change does not build the four ticket screens. The client follows `spec/api-contract.md`, so status stays on the general PATCH. A missing `NEXT_PUBLIC_API_URL` throws before `fetch`. `http://127.0.0.1:3000` is not allowed. A network failure, or a body that is not the shared error shape, throws `ApiError` with message `The server could not be reached.`

## Capabilities

### New Capabilities

- `frontend-api-client`: Call every ticket endpoint and the ask endpoint from the browser, and accept that browser origin.

### Modified Capabilities

## Impact

- New `frontend/` TypeScript package. The Thymeleaf screens stay.
- No CORS configuration exists. A browser call from another origin is blocked by the browser.
- `application.yml` currently sets `server.port` to `8088`. The client learns that host from `NEXT_PUBLIC_API_URL`. `set-backend-port` still owns the move to port 85.
