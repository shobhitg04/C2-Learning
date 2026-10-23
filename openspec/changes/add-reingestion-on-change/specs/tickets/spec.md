# Spec Delta

## MODIFIED Requirements

### Requirement: Re-index after a successful write
A successful create, comment, assignee change, resolution-note change, or allowed status change MUST ingest that ticket only after the ticket transaction commits. Close and cancel are included. A rolled-back ticket write MUST NOT ingest. A forbidden status change or a validation failure MUST leave existing chunks unchanged.

#### Scenario: Replaced text is gone
- **WHEN** a ticket is saved again with different resolution notes and the following ingest succeeds
- **THEN** a later retrieval does not return the previous resolution text for that ticket

#### Scenario: Failed write leaves chunks
- **WHEN** a forbidden status change is requested
- **THEN** the ticket's existing chunks are unchanged

#### Scenario: Rolled-back write does not ingest
- **WHEN** a ticket create rolls back
- **THEN** no chunks are written for that attempt

## ADDED Requirements

### Requirement: Embedding failure keeps the request successful
When ingest runs after a committed ticket write and embedding or the vector replace fails, the system MUST log the error with the ticket id and MUST still return the success status of that write. The previous chunks MUST remain.

#### Scenario: Create still returns 201
- **WHEN** a valid create commits and the following embed fails
- **THEN** the response is `201`, the ticket row exists, and the previous chunks for that id remain

### Requirement: Cancelled tickets stay indexed
A ticket that becomes `CANCELLED` MUST remain in the index, and every chunk stored for that ticket MUST have status metadata `CANCELLED`.

#### Scenario: Cancel keeps chunks
- **WHEN** an `OPEN` ticket becomes `CANCELLED` and the following ingest succeeds
- **THEN** a retrieval can still find that ticket and the stored status is `CANCELLED`
