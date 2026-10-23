# Spec Delta

## Purpose

Turns each ticket into vector documents and replaces that ticket's documents on every ingest, including a full reindex.

## ADDED Requirements

### Requirement: Chunk boundaries
The system MUST build one description chunk from the ticket title and description, one comment chunk per comment, and one resolution chunk only when resolution notes are non-blank.

#### Scenario: Description and one comment
- **WHEN** a ticket has a title, a description, and one comment, and no resolution notes
- **THEN** ingestion stores two chunks, with `chunkType` `description` and `comment`

#### Scenario: Resolution notes present
- **WHEN** a ticket has non-blank resolution notes
- **THEN** ingestion stores a chunk whose `chunkType` is `resolution`

#### Scenario: No resolution notes
- **WHEN** a ticket has no resolution notes
- **THEN** ingestion stores no chunk whose `chunkType` is `resolution`

### Requirement: Chunk text and metadata
Every chunk MUST be stored as a document whose text starts with `Ticket: ` plus the ticket id and `Title: ` plus the title. Metadata MUST contain `ticketId`, `status`, `priority`, `assignee`, `category`, and `chunkType`. A missing assignee MUST be stored as an empty string.

#### Scenario: Description text
- **WHEN** ticket `TKT-1001` has title `Export timeout`, description `Invoice export fails for quarterly reports.`, and no assignee
- **THEN** the description chunk text starts with `Ticket: TKT-1001` and `Title: Export timeout`, the description text follows, and `assignee` is `""`

#### Scenario: Comment text
- **WHEN** that ticket has a comment `Tried CSV and PDF.`
- **THEN** the comment chunk starts with the same ticket id and title, and its body contains `Tried CSV and PDF.`

### Requirement: Replace chunks for one ticket
Re-ingestion MUST delete every stored chunk whose metadata `ticketId` equals that ticket, then insert the new chunks. A later retrieval MUST NOT return the replaced text.

#### Scenario: Old resolution text is gone
- **WHEN** a ticket is ingested again with different resolution notes
- **THEN** a retrieval of that ticket does not return the previous resolution text and does return the new text

### Requirement: Reindex every ticket
`POST /api/ai/reindex` MUST ingest every existing ticket. Each ticket MUST commit or roll back on its own. The response MUST be `200` and MUST include `indexed` and `failed`.

#### Scenario: One ticket fails
- **WHEN** backfill ingests two tickets and the second ingest fails
- **THEN** the response is `200`, `indexed` counts only the ticket that committed, and `failed` contains the id that rolled back
