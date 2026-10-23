# Spec Delta

## MODIFIED Requirements

### Requirement: Create a ticket
The system MUST create an `OPEN` ticket when title is 5 to 120 characters, description is non-blank and at most 5000 characters, and priority and category are non-blank, and MUST return `201` with the stored ticket.

#### Scenario: Valid create
- **WHEN** the client posts title `Export timeout`, a non-blank description of at most 5000 characters, priority `HIGH`, and category `billing` with no assignee
- **THEN** the response is `201`, the status is `OPEN`, the stored title is `Export timeout`, the assignee is null, comments are empty, and resolution notes are null

#### Scenario: Blank description
- **WHEN** the client posts a blank description and a title of 5 to 120 characters
- **THEN** the response is `400`, `fieldErrors` names `description` with message `must not be blank`, and no ticket is stored

#### Scenario: Description too long
- **WHEN** the client posts a description of 5001 characters and a title of 5 to 120 characters
- **THEN** the response is `400`, `fieldErrors` names `description` with message `must be at most 5000 characters`, and no ticket is stored

#### Scenario: Title outside 5 to 120
- **WHEN** the client posts title `Tiny` and a non-blank description, priority, and category
- **THEN** the response is `400`, `fieldErrors` names `title` with message `must be between 5 and 120 characters`, and no ticket is stored

### Requirement: List, search, and filter
The system MUST return a page of ticket summaries. Keyword and status MUST combine with AND. Keyword MUST be a case-insensitive substring of title or description, and MUST NOT match id, category, comment body, or resolution notes.

#### Scenario: First page of one
- **WHEN** the client lists tickets with `page=0` and `size=1` and no filters
- **THEN** the response contains at most one ticket and `total` equals the number of stored tickets

#### Scenario: Keyword and status
- **WHEN** tickets include an `OPEN` billing ticket whose description contains `export` and a `RESOLVED` ticket that does not
- **THEN** `keyword=export` and `status=OPEN` returns only the open billing ticket

#### Scenario: Keyword matches title
- **WHEN** one ticket title contains `payroll` and no description, comment, category, or id contains `payroll`
- **THEN** `keyword=payroll` returns that ticket

#### Scenario: Keyword ignores comments
- **WHEN** a comment body contains `payroll` and no ticket title or description contains `payroll`
- **THEN** `keyword=payroll` returns no tickets

#### Scenario: Page past the end
- **WHEN** the requested page is greater than the last page index
- **THEN** the response is `200`, `content` is empty, and `total` is still the match count

#### Scenario: Non-integer page
- **WHEN** the client lists tickets with `page` set to a non-integer
- **THEN** the response is `400` and `fieldErrors` names `page`

### Requirement: Comment on a ticket
The system MUST append a non-blank comment of at most 2000 characters to an existing ticket and return `201`.

#### Scenario: Comment is stored
- **WHEN** the client posts body `Tried CSV and PDF.` on an existing ticket
- **THEN** the response is `201` and the next read of that ticket includes that comment

#### Scenario: Blank comment
- **WHEN** the client posts a blank comment body
- **THEN** the response is `400` and `fieldErrors` names `body` with message `must not be blank`

#### Scenario: Comment too long
- **WHEN** the client posts a comment body of 2001 characters
- **THEN** the response is `400` and `fieldErrors` names `body` with message `must be at most 2000 characters`

### Requirement: Update assignee and resolution notes
The system MUST change only the title, description, priority, assignee, and resolution notes that the client sends on `PATCH /api/tickets/{id}`. That request MUST NOT change status. An empty update MUST be rejected.

#### Scenario: Set assignee
- **WHEN** the client patches an existing ticket with assignee `sam` and omits status
- **THEN** the response is `200` and the stored assignee is `sam`

#### Scenario: Clear resolution notes
- **WHEN** the client patches `resolutionNotes` as null
- **THEN** the ticket has no resolution notes

#### Scenario: Empty update
- **WHEN** the client patches an existing ticket with an empty object
- **THEN** the response is `400` and `fieldErrors` names `body` with message `must change at least one field`

#### Scenario: Change title
- **WHEN** the client patches title to `Export timeout` and omits every other field
- **THEN** the response is `200` and the stored title is `Export timeout`

#### Scenario: Change description and priority
- **WHEN** the client patches a non-blank description of at most 5000 characters and priority `high`
- **THEN** the response is `200`, the stored description is that text, and the stored priority is `HIGH`

#### Scenario: Title too short
- **WHEN** the client patches title `Tiny`
- **THEN** the response is `400`, `fieldErrors` names `title` with message `must be between 5 and 120 characters`, and the stored title is unchanged

#### Scenario: Status is rejected
- **WHEN** the client includes `status` on `PATCH /api/tickets/{id}`
- **THEN** the response is `400`, `fieldErrors` names `status` with message `must not be set`, and the stored status is unchanged

### Requirement: Allowed status transitions
The system MUST accept a status change only through `PATCH /api/tickets/{id}/status`, and only for the pairs marked Allowed in `spec/state-machine.md`. An accepted pair stores the new status and returns `200`.

#### Scenario: Start work
- **WHEN** the client sends `PATCH /api/tickets/{id}/status` to move an `OPEN` ticket to `IN_PROGRESS`
- **THEN** the response is `200` and the stored status is `IN_PROGRESS`

#### Scenario: Close a resolved ticket
- **WHEN** the client sends `PATCH /api/tickets/{id}/status` to move a `RESOLVED` ticket to `CLOSED`
- **THEN** the response is `200`, the status is `CLOSED`, and existing resolution notes remain

### Requirement: Forbidden status transitions
The system MUST reject every pair marked Forbidden, including a move to the same status, on `PATCH /api/tickets/{id}/status`, with `409` and no stored change.

#### Scenario: No change of status
- **WHEN** the client sends `PATCH /api/tickets/{id}/status` to move an `OPEN` ticket to `OPEN`
- **THEN** the response is `409`, the message is `Status cannot change from OPEN to OPEN`, `fieldErrors` is empty, and the stored status stays `OPEN`

#### Scenario: Close from open
- **WHEN** the client sends `PATCH /api/tickets/{id}/status` to move an `OPEN` ticket to `CLOSED`
- **THEN** the response is `409` and the stored status stays `OPEN`

### Requirement: Ticket screens
The system MUST offer list, detail, create, and edit screens that call the ticket API and show server field messages on the matching fields. The edit screen MUST send a status change only to `PATCH /api/tickets/{id}/status`.

#### Scenario: Create screen
- **WHEN** the user submits a blank description on the create screen
- **THEN** the description field shows `must not be blank` and no create request is sent

#### Scenario: Short title on the create screen
- **WHEN** the user submits title `Tiny` and a non-blank description on the create screen
- **THEN** the title field shows `must be between 5 and 120 characters` and no create request is sent

#### Scenario: Edit status choices
- **WHEN** the user opens edit for an `OPEN` ticket
- **THEN** the status control offers the current status and only the targets allowed from `OPEN`

#### Scenario: Edit status uses the status endpoint
- **WHEN** the user changes the status on the edit screen and saves
- **THEN** the status request goes to `PATCH /api/tickets/{id}/status` and the general ticket update does not include `status`
