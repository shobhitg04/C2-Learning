# Spec Delta

## ADDED Requirements

### Requirement: Create ticket form
`/tickets/new` MUST collect title, description, priority, and assignee. Priority MUST be `HIGH`, `MEDIUM`, or `LOW`. The form MUST NOT send category, status, or resolution notes. An empty assignee MUST be omitted. A blank title, description, or priority MUST show `must not be blank` under that field and MUST NOT call the API. Success MUST open `/tickets/{id}` for the returned id.

#### Scenario: Create opens the new ticket
- **WHEN** the user submits a title, a description, and priority `HIGH` with assignee left empty
- **THEN** the client posts those three fields and opens the detail page for the returned id

### Requirement: Ticket detail
`/tickets/{id}` MUST show the id, title, description, status, priority, and assignee. A null assignee MUST be shown as `Unassigned`. Comments MUST appear in response order with `body` and `createdAt`. An add-comment form MUST call `POST /api/tickets/{id}/comments`. A blank comment MUST show `must not be blank` and MUST NOT call the API. Success MUST append the returned comment and clear the box.

#### Scenario: Comment is appended
- **WHEN** the user submits `Tried CSV and PDF.` on the detail page
- **THEN** that comment appears and the box is empty

### Requirement: Status control
The detail page MUST offer only the status values `spec/state-machine.md` marks Allowed from the loaded status. Submitting a choice MUST send only that status. A `409` MUST show the backend `message` in a visible alert and MUST leave the loaded ticket unchanged on screen.

#### Scenario: Allowed target is offered
- **WHEN** the loaded status is `OPEN` and `spec/state-machine.md` marks `OPEN` to `IN_PROGRESS` Allowed
- **THEN** the control includes `IN_PROGRESS` and does not include `OPEN`

#### Scenario: Invalid transition alert
- **WHEN** a status change returns `409` with message `Status cannot change from OPEN to OPEN`
- **THEN** that message is shown in a visible alert

### Requirement: Edit ticket form
`/tickets/{id}/edit` MUST edit title, description, priority, and assignee. It MUST NOT edit category, status, or resolution notes. An emptied assignee MUST send `null`. A blank title or description MUST show `must not be blank` and MUST NOT call the API. Success MUST open the detail page.

#### Scenario: Edit saves the title
- **WHEN** the user changes the title and submits
- **THEN** the client patches the title and opens `/tickets/{id}`

### Requirement: Field errors
A `400` `fieldErrors` entry MUST be shown under the input named by `field`, using `message` unchanged. It MUST NOT be the only copy in the `409` alert.

#### Scenario: Description error stays on the field
- **WHEN** create returns `400` with `fieldErrors` field `description` and message `must not be blank`
- **THEN** that message is shown under the description input
