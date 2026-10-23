# Proposal

## Why

`spec/state-machine.md` still allows reopen and a direct jump from `OPEN` to `RESOLVED`. The ticket workflow should allow only five moves, and every other pair should fail in a pure domain class that unit tests can call without Spring or a database.

## What Changes

- Add `TicketStateMachine` in `tickets.domain`. It allows only `OPEN` to `IN_PROGRESS`, `IN_PROGRESS` to `RESOLVED`, `RESOLVED` to `CLOSED`, `OPEN` to `CANCELLED`, and `IN_PROGRESS` to `CANCELLED`.
- Every other pair throws `InvalidTransitionException`, including a move to the same status, any move out of `CLOSED` or `CANCELLED`, `RESOLVED` to `OPEN`, `OPEN` to `RESOLVED`, and `IN_PROGRESS` to `OPEN`.
- **BREAKING:** Those last five pairs return `200` today. They return `409`, leave the row unchanged, and do not re-index.
- Update `spec/state-machine.md` so its diagram, matrix, side effects, and trade-off match these five allowed pairs.
- Replace `StatusPolicy` with this class. The edit screen offers only the targets this class allows.

Assumption: the class lives in `tickets.domain`, the app's existing domain package. The exception is unchecked and its message is `Status cannot change from {from} to {to}`.

## Capabilities

### New Capabilities

### Modified Capabilities

- `tickets`: The allowed status set shrinks to the five pairs above. `OPEN` to `RESOLVED`, `IN_PROGRESS` to `OPEN`, `RESOLVED` to `OPEN`, `CLOSED` to `OPEN`, and `CANCELLED` to `OPEN` become `409`.

## Impact

- `StatusPolicy`, `TicketService.patch`, and `TicketPageController` are the current callers. API error bodies stay the shared `409` shape.
- `StatusPolicyTest` currently expects the five removed pairs to be allowed. Those tests move to the exception.
- `spec/test-strategy.md` lists those five as allow tests. It is updated with `spec/state-machine.md`.
- The in-progress change `add-ticket-persistence` may rename `Status` to `TicketStatus`. This change uses whichever status enum is already in `tickets.domain` and does not add a second one. It does not change title, priority, or timestamps.
- No new dependency.
