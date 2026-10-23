# Spec Delta

## ADDED Requirements

### Requirement: Ticket list page
The site MUST show the ticket list at `/`. The table columns MUST be Key, Title, Status, Priority, and Assignee. Key is the ticket id. Title is the ticket description. A null assignee MUST be shown as `Unassigned`. The page MUST NOT show category. The first load MUST request `GET /api/tickets` with `page` `0` and `size` `20`.

#### Scenario: First page loads
- **WHEN** the user opens `/`
- **THEN** the client requests page `0` and size `20`, and each row shows that ticket's id, description, status, priority, and assignee

### Requirement: Search and pagination
A keyword box, a status dropdown, and a Search button MUST be on the page. Search MUST run when the button is used, not on each keystroke. `All` MUST omit `status`. A blank keyword MUST omit `keyword`. Previous and Next MUST change `page` by one. Previous MUST be disabled on page `0`. Next MUST be disabled when `(page + 1) * size` is greater than or equal to `total`. The label MUST be `Page {page + 1}`.

#### Scenario: Search sends both filters
- **WHEN** the user enters `export`, chooses `OPEN`, and presses Search
- **THEN** the request includes `keyword=export`, `status=OPEN`, `page=0`, and `size=20`

#### Scenario: Next page
- **WHEN** the user presses Next on page `0` and another page exists
- **THEN** the client requests page `1` with the same keyword, status, and size

### Requirement: Loading, empty, and error
While a list request is in flight, the page MUST show `Loading tickets.` An empty `content` array after a successful response MUST show `No tickets match.` and no rows. A failed request MUST show a visible banner with the error message and MUST NOT show the empty state.

#### Scenario: No rows
- **WHEN** a successful response has `content` `[]`
- **THEN** the page shows `No tickets match.` and no table rows

#### Scenario: API failure
- **WHEN** the list request fails
- **THEN** a banner shows the error message and the page does not show `No tickets match.`
