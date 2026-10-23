# Spec Delta

## ADDED Requirements

### Requirement: Ask with a question
`POST /api/ai/ask` MUST accept a JSON body `{ "question": "..." }`, trim `question`, and return `200` with `answer`, `sources`, and `noRelevantTickets`. The endpoint MUST NOT create a ticket, add a comment, change status, change assignee, or write resolution notes. Unknown properties are ignored.

#### Scenario: Grounded answer
- **WHEN** the question matches a stored ticket at or above the similarity threshold and the model cites only that ticket
- **THEN** the response is `200`, `noRelevantTickets` is `false`, and `sources` contains that ticket once

### Requirement: Retrieval limits
The search MUST read `app.rag.top-k` and `app.rag.similarity-threshold` from configuration. It MUST consider at most that many nearest chunks, MUST omit chunks below the threshold, and MUST treat a higher score as closer. A cosine distance MUST be converted with `similarity = 1 - distance` before that comparison.

#### Scenario: Threshold drops a near miss
- **WHEN** the nearest chunk scores below the configured threshold
- **THEN** that chunk is not a source and is not sent to the chat model

### Requirement: One source per ticket
Passing chunks MUST be grouped by ticket. Each source has `ticketId`, `title` equal to that ticket's description, and `similarity` equal to the highest passing chunk score for that ticket. Sources MUST be ordered by `similarity` descending, then `ticketId` ascending.

#### Scenario: Two chunks collapse
- **WHEN** two chunks of the same ticket both pass the threshold
- **THEN** `sources` contains that ticket once and `similarity` is the higher score

### Requirement: No match skips the model
When no chunk is at or above the threshold, the service MUST NOT call the chat model. The response MUST be `200`, `sources` MUST be `[]`, `noRelevantTickets` MUST be `true`, and `answer` MUST be exactly `No relevant tickets found.`

#### Scenario: Empty retrieval
- **WHEN** every considered chunk is below the threshold
- **THEN** the response is that no-match body and the chat model was not called

### Requirement: Grounded chat answer
When at least one chunk passes, the service MUST call the chat model with the static system prompt from the grounding section of `spec/rag-api-contract.md` and with only those chunk texts as ticket context. The model name MUST come from `spring.ai.ollama.chat.options.model`. A grounded answer MUST keep the retrieved sources and `noRelevantTickets` `false`. Every `TKT-` id in the answer MUST be one of those source ids.

#### Scenario: Citation uses a retrieved id
- **WHEN** the model answers from the supplied chunks and cites a retrieved ticket id
- **THEN** the response returns that answer and the retrieved sources

### Requirement: Ungrounded answer is discarded
If the model text contains a `TKT-` id that was not retrieved, the service MUST log a warning that includes that id, MUST discard the model text, and MUST return the no-match body. If the model text is exactly `No relevant tickets found.`, the service MUST return that same body.

#### Scenario: Invented ticket id
- **WHEN** the model cites a ticket id that was not in the retrieved set
- **THEN** the response is the no-match body and that id is not a source

#### Scenario: Model declines
- **WHEN** the model replies exactly `No relevant tickets found.`
- **THEN** `sources` is `[]` and `noRelevantTickets` is `true`

### Requirement: Ask errors
A missing or blank `question` MUST return `400` with `fieldErrors` entry `question` and message `must not be blank`. When the question cannot be embedded, or the chat call fails after chunks were retrieved, the response MUST be `503`, `error` `Service Unavailable`, message `Question could not be answered`, and `fieldErrors` `[]`. Either failure MUST leave tickets, comments, statuses, assignees, and resolution notes unchanged.

#### Scenario: Blank question
- **WHEN** `question` is missing or blank after trim
- **THEN** the response is `400`, the field is `question`, and the message is `must not be blank`

#### Scenario: Chat failure writes nothing
- **WHEN** chunks were retrieved and the chat call fails
- **THEN** the response is `503` with message `Question could not be answered` and the stored tickets are unchanged
