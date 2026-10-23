# Spec Delta

## ADDED Requirements

### Requirement: Typed ticket client
The browser client MUST call `POST /api/tickets`, `GET /api/tickets`, `GET /api/tickets/{id}`, `PATCH /api/tickets/{id}`, and `POST /api/tickets/{id}/comments` on the base URL from `NEXT_PUBLIC_API_URL`. It MUST send and return the JSON shapes in `spec/api-contract.md`. List requests MUST send only the page, size, keyword, and status values the caller provides. Patch requests MUST send only the fields the caller provides.

#### Scenario: Create returns the ticket
- **WHEN** the client creates a ticket and the server returns `201`
- **THEN** the result is the ticket detail, including the returned id

#### Scenario: List sends the query
- **WHEN** the client lists tickets with keyword `export`, status `OPEN`, page `0`, and size `20`
- **THEN** the request is `GET` with those query parameters and the result is the summary page

### Requirement: Typed ask client
The browser client MUST call `POST /api/ai/ask` with `{ "question": "..." }` and MUST return `answer`, `sources`, and `noRelevantTickets` as specified in `spec/rag-api-contract.md`. It MUST NOT send ticket fields.

#### Scenario: Ask returns sources
- **WHEN** the server returns `200` with one source and `noRelevantTickets` false
- **THEN** the client returns that answer, that source, and `noRelevantTickets` false

### Requirement: API errors
When the response status is not a success status, the client MUST parse the shared error body and throw an error that keeps `status`, `message`, and `fieldErrors`. A network failure, or a body that is not that shape, MUST throw an error whose message is `The server could not be reached.` A missing `NEXT_PUBLIC_API_URL` MUST throw before any request.

#### Scenario: Validation error
- **WHEN** create returns `400` with one `fieldErrors` entry for `description`
- **THEN** the thrown error has status `400`, the server message, and that field error

#### Scenario: Index failure
- **WHEN** a ticket write returns `503` with message `Ticket could not be indexed`
- **THEN** the thrown error has status `503`, that message, and an empty `fieldErrors`

### Requirement: Local frontend origin
The API MUST accept browser requests from `http://localhost:3000` and MUST reject every other origin. Allowed methods are `GET`, `POST`, `PATCH`, and `OPTIONS`.

#### Scenario: Local origin is allowed
- **WHEN** the browser sends a preflight from `http://localhost:3000` for `POST /api/tickets`
- **THEN** the response allows that origin

#### Scenario: Other origin is rejected
- **WHEN** the browser sends a preflight from `http://localhost:3001`
- **THEN** the response does not allow that origin
