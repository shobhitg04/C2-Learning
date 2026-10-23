# Spec Delta

## ADDED Requirements

### Requirement: Dedicated status endpoint
The system MUST accept `PATCH /api/tickets/{id}/status` with a `status` field. An allowed pair stores the new status and returns `200`. An id that matches no ticket returns `404`.

#### Scenario: Start work
- **WHEN** the client sends `PATCH /api/tickets/{id}/status` to move an `OPEN` ticket to `IN_PROGRESS`
- **THEN** the response is `200` and the stored status is `IN_PROGRESS`

#### Scenario: Unknown ticket
- **WHEN** the client sends `PATCH /api/tickets/{id}/status` for an id that matches no ticket
- **THEN** the response is `404`, the message is `Ticket ` plus that id plus ` not found`, and `fieldErrors` is empty

#### Scenario: Status value is not a status
- **WHEN** the client sends `status` `DONE` on that endpoint
- **THEN** the response is `400`, `fieldErrors` names `status`, and the stored status is unchanged

## MODIFIED Requirements

### Requirement: Shared error body
Every `400`, `404`, and `409` response MUST contain only `timestamp`, `status`, `error`, `message`, and `fieldErrors`. A validation failure MUST return `400` with one `fieldErrors` entry per invalid field. An unknown ticket MUST return `404` with `fieldErrors` empty. An invalid status transition MUST return `409` with `fieldErrors` empty and a message that names the stored status and the requested status.

#### Scenario: Validation error shape
- **WHEN** a create request fails validation
- **THEN** the body has those five fields and `fieldErrors` has one entry per invalid field

#### Scenario: Unknown ticket shape
- **WHEN** the client gets an id that matches no ticket
- **THEN** the body has those five fields, `status` is `404`, `fieldErrors` is empty, and the message is `Ticket ` plus that id plus ` not found`

#### Scenario: Invalid transition shape
- **WHEN** an `OPEN` ticket is moved to `OPEN`
- **THEN** the body has those five fields, `status` is `409`, `error` is `Conflict`, `fieldErrors` is empty, and the message is `Status cannot change from OPEN to OPEN`
