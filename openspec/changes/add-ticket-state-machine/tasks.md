# Tasks

## 1. Domain machine

- [ ] 1.1 Update `spec/state-machine.md` so the diagram, matrix, side-effect table, and trade-off allow only `OPEN` to `IN_PROGRESS`, `IN_PROGRESS` to `RESOLVED`, `RESOLVED` to `CLOSED`, `OPEN` to `CANCELLED`, and `IN_PROGRESS` to `CANCELLED`. Mark every other pair Forbidden, including any move out of `CLOSED` or `CANCELLED`. Verify the matrix has exactly those five Allowed cells.
- [ ] 1.2 Add `tickets.domain.TicketStateMachine` and unchecked `InvalidTransitionException` with message `Status cannot change from {from} to {to}`. `require` throws for every pair outside the five. `allowedTargets` returns only the legal targets. Use the existing status enum (`Status`, or `TicketStatus` if `add-ticket-persistence` already replaced it). Delete `StatusPolicy`. Verify unit tests for all 25 pairs: the five allowed pairs do not throw, and the other 20 throw that message. Update the matching rows in `spec/test-strategy.md` and verify the five former allow names are now reject names.

## 2. API and edit screen

- [ ] 2.1 Catch `InvalidTransitionException` in `TicketService.patch` and return `409` with that message, an empty `fieldErrors`, no row change, and no re-index. Verify `OPEN` to `IN_PROGRESS` returns `200`, `RESOLVED` to `CLOSED` returns `200` and keeps resolution notes, and `OPEN` to `RESOLVED` returns `409` with message `Status cannot change from OPEN to RESOLVED` while the stored status stays `OPEN`.
- [ ] 2.2 Fill the edit status control from `allowedTargets`. For an `OPEN` ticket the control offers the current status, `IN_PROGRESS`, and `CANCELLED` only. If `spec/ui-flow.md` lists `OPEN` to `RESOLVED` or a reopen, update it and verify those lines are gone. Verify a slice test of the `OPEN` edit page contains `IN_PROGRESS` and `CANCELLED` and does not offer `RESOLVED` as a target.

## Workflow follow-up

- Review the narrowed matrix against `spec/state-machine.md` before archive.
- Archive with `/opsx-archive` after implementation is accepted.
