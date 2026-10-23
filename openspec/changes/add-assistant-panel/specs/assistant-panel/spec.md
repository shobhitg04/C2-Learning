# Spec Delta

## ADDED Requirements

### Requirement: Assistant panel
The ticket screens MUST show an assistant side panel with a question box and an Ask button. Ask MUST call `POST /api/ai/ask` with only the question. It MUST NOT send ticket fields, and it MUST NOT copy the question from the open ticket. A blank question MUST show `must not be blank` and MUST NOT call the API. There MUST NOT be a separate `/assistant` page.

#### Scenario: Ask sends only the question
- **WHEN** the user asks `Why do quarterly invoice exports fail?`
- **THEN** the request body is that question and no ticket field

### Requirement: Answer and sources
A grounded response MUST show `answer` verbatim, then one source per item in response order. Each source MUST show `ticketId`, `title`, and `similarity`. The ticket key MUST link to `/tickets/{ticketId}`. Asking MUST NOT change the ticket page behind the panel.

#### Scenario: Source link opens the ticket
- **WHEN** the response cites `TKT-1001` and `noRelevantTickets` is false
- **THEN** the panel shows the answer and a source whose key links to `/tickets/TKT-1001`

### Requirement: No matching tickets
When `noRelevantTickets` is true, the panel MUST show the server answer as a distinct no-match message and MUST NOT show a source list. The message MUST be the server string, including the final period. It MUST NOT be shown as an empty answer or as an error.

#### Scenario: No match is distinct
- **WHEN** the response answer is `No relevant tickets found.` and `noRelevantTickets` is true
- **THEN** that sentence is shown as the no-match message, with no sources and no error

### Requirement: Loading and errors
While Ask is in flight, the panel MUST show a loading indicator and MUST disable Ask. A failed request MUST show the error in the panel and MUST leave the previous answer and no-match message in place. A `503` uses the message `Question could not be answered`. A network failure uses `The server could not be reached.`

#### Scenario: Failure keeps the previous answer
- **WHEN** a later Ask returns `503` after a grounded answer was shown
- **THEN** the panel shows `Question could not be answered` and the previous answer remains
