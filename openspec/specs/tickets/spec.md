# tickets Specification

## Purpose

Lets support staff create, find, comment on, and advance tickets through a fixed set of statuses.

## Requirements

### Requirement: Create a ticket
The system MUST create an `OPEN` ticket when description, priority, and category are non-blank, and MUST return `201` with the stored ticket.

#### Scenario: Valid create
- **WHEN** the client posts a non-blank description, priority `HIGH`, and category `billing` with no assignee
- **THEN** the response is `201`, the status is `OPEN`, the assignee is null, comments are empty, and resolution notes are null

#### Scenario: Blank description
- **WHEN** the client posts a blank description
- **THEN** the response is `400`, `fieldErrors` names `description` with message `must not be blank`, and no ticket is stored

### Requirement: Priority values
The system MUST accept only `HIGH`, `MEDIUM`, and `LOW` as priority, using trim and uppercase before the check.

#### Scenario: Lowercase priority
- **WHEN** the client posts priority `high` with a valid description and category
- **THEN** the response is `201` and the stored priority is `HIGH`

#### Scenario: Unknown priority
- **WHEN** the client posts priority `URGENT`
- **THEN** the response is `400` and `fieldErrors` names `priority` with message `must be HIGH, MEDIUM, or LOW`

### Requirement: Ticket identifiers
On an empty database the system MUST assign `TKT-1001` to the first ticket and the next integer to each later ticket.

#### Scenario: First ticket
- **WHEN** the client creates a ticket and no tickets exist
- **THEN** the returned id is `TKT-1001`

### Requirement: Read a ticket
The system MUST return the ticket, its comments in creation order, and its resolution notes for a known id.

#### Scenario: Known id
- **WHEN** the client gets `TKT-1001` and that ticket exists
- **THEN** the response is `200` and includes description, status, priority, assignee, category, comments, and resolution notes

#### Scenario: Unknown id
- **WHEN** the client gets an id that matches no ticket
- **THEN** the response is `404` and the message is `Ticket ` plus that id plus ` not found`

#### Scenario: Malformed id
- **WHEN** the client gets an id that is not `TKT-` plus digits
- **THEN** the response is `400` and `fieldErrors` names `id` with message `must match TKT- and digits`

### Requirement: List, search, and filter
The system MUST return a page of ticket summaries. Keyword and status MUST combine with AND. Keyword MUST be a case-insensitive substring of id, description, category, comment body, or resolution notes.

#### Scenario: First page of one
- **WHEN** the client lists tickets with `page=0` and `size=1` and no filters
- **THEN** the response contains at most one ticket and `total` equals the number of stored tickets

#### Scenario: Keyword and status
- **WHEN** tickets include an `OPEN` billing ticket whose description contains `export` and a `RESOLVED` ticket that does not
- **THEN** `keyword=export` and `status=OPEN` returns only the open billing ticket

#### Scenario: Page past the end
- **WHEN** the requested page is greater than the last page index
- **THEN** the response is `200`, `content` is empty, and `total` is still the match count

#### Scenario: Non-integer page
- **WHEN** the client lists tickets with `page` set to a non-integer
- **THEN** the response is `400` and `fieldErrors` names `page`

### Requirement: Comment on a ticket
The system MUST append a non-blank comment to an existing ticket and return `201`.

#### Scenario: Comment is stored
- **WHEN** the client posts body `Tried CSV and PDF.` on an existing ticket
- **THEN** the response is `201` and the next read of that ticket includes that comment

#### Scenario: Blank comment
- **WHEN** the client posts a blank comment body
- **THEN** the response is `400` and `fieldErrors` names `body` with message `must not be blank`

### Requirement: Update assignee and resolution notes
The system MUST change only the assignee and resolution notes that the client sends. An empty update MUST be rejected.

#### Scenario: Set assignee
- **WHEN** the client patches an existing ticket with assignee `sam` and omits status
- **THEN** the response is `200` and the stored assignee is `sam`

#### Scenario: Clear resolution notes
- **WHEN** the client patches `resolutionNotes` as null
- **THEN** the ticket has no resolution notes

#### Scenario: Empty update
- **WHEN** the client patches an existing ticket with an empty object
- **THEN** the response is `400` and `fieldErrors` names `body` with message `must change at least one field`

### Requirement: Allowed status transitions
The system MUST accept only the pairs marked Allowed in `spec/state-machine.md`, store the new status, and return `200`.

#### Scenario: Start work
- **WHEN** an `OPEN` ticket is patched to `IN_PROGRESS`
- **THEN** the response is `200` and the stored status is `IN_PROGRESS`

#### Scenario: Close a resolved ticket
- **WHEN** a `RESOLVED` ticket is patched to `CLOSED`
- **THEN** the response is `200`, the status is `CLOSED`, and existing resolution notes remain

### Requirement: Forbidden status transitions
The system MUST reject every pair marked Forbidden, including a move to the same status, with `409` and no stored change.

#### Scenario: No change of status
- **WHEN** an `OPEN` ticket is patched to `OPEN`
- **THEN** the response is `409`, the message is `Status cannot change from OPEN to OPEN`, `fieldErrors` is empty, and the stored status stays `OPEN`

#### Scenario: Close from open
- **WHEN** an `OPEN` ticket is patched to `CLOSED`
- **THEN** the response is `409` and the stored status stays `OPEN`

### Requirement: Shared error body
Every `400`, `404`, and `409` response MUST contain only `timestamp`, `status`, `error`, `message`, and `fieldErrors`.

#### Scenario: Validation error shape
- **WHEN** a create request fails validation
- **THEN** the body has those five fields and `fieldErrors` has one entry per invalid field

### Requirement: Re-index after a successful write
A successful create, comment, assignee change, resolution-note change, or allowed status change MUST replace that ticket's knowledge chunks before the write is visible.

#### Scenario: Replaced text is gone
- **WHEN** a ticket is saved again with different resolution notes
- **THEN** a later retrieval does not return the previous resolution text for that ticket

#### Scenario: Failed write leaves chunks
- **WHEN** a forbidden status change is requested
- **THEN** the ticket's existing chunks are unchanged

### Requirement: Ticket screens
The system MUST offer list, detail, create, and edit screens that call the ticket API and show server field messages on the matching fields.

#### Scenario: Create screen
- **WHEN** the user submits a blank description on the create screen
- **THEN** the description field shows `must not be blank` and no create request is sent

#### Scenario: Edit status choices
- **WHEN** the user opens edit for an `OPEN` ticket
- **THEN** the status control offers the current status and only the targets allowed from `OPEN`
