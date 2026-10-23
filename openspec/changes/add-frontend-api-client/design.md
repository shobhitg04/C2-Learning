# Design

## Context

`frontend/` does not exist. Ticket routes and `POST /api/ai/ask` are implemented. Errors use `{timestamp, status, error, message, fieldErrors[]}` from `ApiErrors`. Nothing configures CORS. See proposal.md for the screens and the status route that stay out of this change.

## Goals / Non-Goals

**Goals:**

- One typed module, `frontend/src/lib/api.ts`, for the five ticket calls and ask.
- Throw `ApiError` for every non-ok response.
- Allow only `http://localhost:3000`.

**Non-Goals:**

- The list, detail, create, edit, and assistant screens.
- `PATCH /api/tickets/{id}/status`.
- Replacing the Thymeleaf pages.

## Decisions

### One fetch wrapper

`frontend/` is a TypeScript package with `package.json` and `tsconfig.json` so `tsc --noEmit` checks the client. It does not add ticket pages.

`api.ts` exports `createTicket`, `listTickets`, `getTicket`, `updateTicket`, `addComment`, and `ask`. Each function builds `new URL(path, NEXT_PUBLIC_API_URL)`, sends JSON, and returns the parsed success body. Types match the contract: ticket detail, ticket summary page, comment, and ask response. `updateTicket` omits keys the caller did not set, so a null assignee is sent and an omitted assignee is not.

`ApiError` extends `Error` and stores `status`, `message`, and `fieldErrors`. A non-ok body with those fields uses them. `fieldErrors` defaults to `[]`. Any other failure uses message `The server could not be reached.` An empty `NEXT_PUBLIC_API_URL` throws that same error before `fetch`.

The example env file sets `NEXT_PUBLIC_API_URL` to the origin of the current `server.port`. The value is not written in `api.ts`.

Alternative: generate the client from an OpenAPI document. Rejected because the backend does not publish one, and the contract is six calls.

### CORS for one origin

A `WebMvcConfigurer` maps `/api/**` to allowed origin `http://localhost:3000`, methods `GET`, `POST`, `PATCH`, and `OPTIONS`, and header `Content-Type`. Credentials stay off. No other origin is listed.

Alternative: `@CrossOrigin` on each controller. Rejected because a second controller would have to repeat the origin.

## Risks / Trade-offs

- [A page opened as `http://127.0.0.1:3000` is blocked] → The request allows only `http://localhost:3000`.
- [The example URL follows today's `server.port` of `8088`] → Changing the port means changing `NEXT_PUBLIC_API_URL`, not the client.

## Migration Plan

1. Add the package, the client, and the CORS mapping.
2. Roll back by removing `frontend/` and the CORS mapping. Ticket data is unchanged.

## Open Questions

None. The client covers the six routes in the ticket and ask contracts.
