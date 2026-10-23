# Spec Delta

## MODIFIED Requirements

### Requirement: Allowed status transitions
The system MUST accept only `OPEN` to `IN_PROGRESS`, `IN_PROGRESS` to `RESOLVED`, `RESOLVED` to `CLOSED`, `OPEN` to `CANCELLED`, and `IN_PROGRESS` to `CANCELLED`. An accepted pair stores the new status and returns `200`. `spec/state-machine.md` MUST mark those five pairs Allowed and every other pair Forbidden.

#### Scenario: Start work
- **WHEN** an `OPEN` ticket is patched to `IN_PROGRESS`
- **THEN** the response is `200` and the stored status is `IN_PROGRESS`

#### Scenario: Resolve in-progress work
- **WHEN** an `IN_PROGRESS` ticket is patched to `RESOLVED`
- **THEN** the response is `200` and the stored status is `RESOLVED`

#### Scenario: Close a resolved ticket
- **WHEN** a `RESOLVED` ticket is patched to `CLOSED`
- **THEN** the response is `200`, the status is `CLOSED`, and existing resolution notes remain

#### Scenario: Cancel an open ticket
- **WHEN** an `OPEN` ticket is patched to `CANCELLED`
- **THEN** the response is `200` and the stored status is `CANCELLED`

#### Scenario: Cancel in-progress work
- **WHEN** an `IN_PROGRESS` ticket is patched to `CANCELLED`
- **THEN** the response is `200` and the stored status is `CANCELLED`

### Requirement: Forbidden status transitions
The system MUST reject every pair other than the five allowed pairs, including a move to the same status, any move out of `CLOSED` or `CANCELLED`, `RESOLVED` to `OPEN`, `OPEN` to `RESOLVED`, and `IN_PROGRESS` to `OPEN`, with `409` and no stored change.

#### Scenario: No change of status
- **WHEN** an `OPEN` ticket is patched to `OPEN`
- **THEN** the response is `409`, the message is `Status cannot change from OPEN to OPEN`, `fieldErrors` is empty, and the stored status stays `OPEN`

#### Scenario: Close from open
- **WHEN** an `OPEN` ticket is patched to `CLOSED`
- **THEN** the response is `409` and the stored status stays `OPEN`

#### Scenario: Resolve directly from open
- **WHEN** an `OPEN` ticket is patched to `RESOLVED`
- **THEN** the response is `409`, the message is `Status cannot change from OPEN to RESOLVED`, and the stored status stays `OPEN`

#### Scenario: Send in-progress work back
- **WHEN** an `IN_PROGRESS` ticket is patched to `OPEN`
- **THEN** the response is `409` and the stored status stays `IN_PROGRESS`

#### Scenario: Reopen a resolved ticket
- **WHEN** a `RESOLVED` ticket is patched to `OPEN`
- **THEN** the response is `409` and the stored status stays `RESOLVED`

#### Scenario: Reopen a closed ticket
- **WHEN** a `CLOSED` ticket is patched to `OPEN`
- **THEN** the response is `409` and the stored status stays `CLOSED`

#### Scenario: Reopen a cancelled ticket
- **WHEN** a `CANCELLED` ticket is patched to `OPEN`
- **THEN** the response is `409` and the stored status stays `CANCELLED`
