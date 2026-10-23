# Spec Delta

## MODIFIED Requirements

### Requirement: Create a ticket
The system MUST create an `OPEN` ticket when title, description, priority, and category are non-blank, and MUST return `201` with the stored ticket.

#### Scenario: Valid create
- **WHEN** the client posts a non-blank title `Export timeout`, a non-blank description, priority `HIGH`, and category `billing` with no assignee
- **THEN** the response is `201`, the status is `OPEN`, the assignee is null, comments are empty, resolution notes are null, and the stored title is `Export timeout`

#### Scenario: Blank description
- **WHEN** the client posts a blank description and a non-blank title
- **THEN** the response is `400`, `fieldErrors` names `description` with message `must not be blank`, and no ticket is stored

#### Scenario: Blank title
- **WHEN** the client posts a blank title and a non-blank description, priority, and category
- **THEN** the response is `400`, `fieldErrors` names `title` with message `must not be blank`, and no ticket is stored

### Requirement: Priority values
The system MUST accept only `LOW`, `MEDIUM`, `HIGH`, and `CRITICAL` as priority, using trim and uppercase before the check.

#### Scenario: Lowercase priority
- **WHEN** the client posts priority `high` with a valid title, description, and category
- **THEN** the response is `201` and the stored priority is `HIGH`

#### Scenario: Critical priority
- **WHEN** the client posts priority `critical` with a valid title, description, and category
- **THEN** the response is `201` and the stored priority is `CRITICAL`

#### Scenario: Unknown priority
- **WHEN** the client posts priority `URGENT`
- **THEN** the response is `400` and `fieldErrors` names `priority` with message `must be LOW, MEDIUM, HIGH, or CRITICAL`

### Requirement: Read a ticket
The system MUST return the ticket, its comments in creation order, and its resolution notes for a known id.

#### Scenario: Known id
- **WHEN** the client gets `TKT-1001` and that ticket exists
- **THEN** the response is `200` and includes title, description, status, priority, assignee, category, comments, resolution notes, `createdAt`, and `updatedAt`

#### Scenario: Unknown id
- **WHEN** the client gets an id that matches no ticket
- **THEN** the response is `404` and the message is `Ticket ` plus that id plus ` not found`

#### Scenario: Malformed id
- **WHEN** the client gets an id that is not `TKT-` plus digits
- **THEN** the response is `400` and `fieldErrors` names `id` with message `must match TKT- and digits`

### Requirement: List, search, and filter
The system MUST return a page of ticket summaries. Keyword and status MUST combine with AND. Keyword MUST be a case-insensitive substring of id, title, description, category, comment body, or resolution notes.

#### Scenario: First page of one
- **WHEN** the client lists tickets with `page=0` and `size=1` and no filters
- **THEN** the response contains at most one ticket and `total` equals the number of stored tickets

#### Scenario: Keyword and status
- **WHEN** tickets include an `OPEN` billing ticket whose description contains `export` and a `RESOLVED` ticket that does not
- **THEN** `keyword=export` and `status=OPEN` returns only the open billing ticket

#### Scenario: Keyword matches title
- **WHEN** one ticket title contains `payroll` and no other ticket text contains `payroll`
- **THEN** `keyword=payroll` returns that ticket

#### Scenario: Page past the end
- **WHEN** the requested page is greater than the last page index
- **THEN** the response is `200`, `content` is empty, and `total` is still the match count

#### Scenario: Non-integer page
- **WHEN** the client lists tickets with `page` set to a non-integer
- **THEN** the response is `400` and `fieldErrors` names `page`

### Requirement: Ticket screens
The system MUST offer list, detail, create, and edit screens that call the ticket API and show server field messages on the matching fields.

#### Scenario: Create screen
- **WHEN** the user submits a blank description on the create screen
- **THEN** the description field shows `must not be blank` and no create request is sent

#### Scenario: Blank title on the create screen
- **WHEN** the user submits a blank title and a non-blank description on the create screen
- **THEN** the title field shows `must not be blank` and no create request is sent

#### Scenario: Edit status choices
- **WHEN** the user opens edit for an `OPEN` ticket
- **THEN** the status control offers the current status and only the targets allowed from `OPEN`

## ADDED Requirements

### Requirement: Ticket timestamps
The system MUST set `createdAt` once when the ticket is stored and MUST set `updatedAt` to that same instant. A later patch that changes the ticket MUST change `updatedAt` and MUST leave `createdAt` unchanged. Appending a comment MUST leave both instants unchanged.

#### Scenario: Create stamps both instants
- **WHEN** the client creates a valid ticket
- **THEN** `createdAt` and `updatedAt` are present and equal

#### Scenario: Patch moves updatedAt
- **WHEN** the client patches the assignee of an existing ticket
- **THEN** `createdAt` is unchanged and `updatedAt` is later than it was before the patch

#### Scenario: Comment leaves ticket instants
- **WHEN** the client posts a comment on an existing ticket
- **THEN** that ticket's `createdAt` and `updatedAt` stay the same
