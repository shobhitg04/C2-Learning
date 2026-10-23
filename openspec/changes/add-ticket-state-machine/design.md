# Design

## Context

`tickets.domain.StatusPolicy` is a static set of ten allowed pairs and returns a boolean. `TicketService.patch` turns a false result into `409` with message `Status cannot change from {from} to {to}` and does not re-index. `TicketPageController` uses the boolean to fill the edit status control. See proposal.md for why the allowed set shrinks. The behavior is in `specs/tickets/spec.md`.

## Goals / Non-Goals

**Goals:**

- One pure class owns the five allowed pairs and throws on every other pair.
- Unit tests call that class with no Spring context and no database.

**Non-Goals:**

- Adding a transition that is not in the five pairs.
- Treating create, comment, or resolution-note edits as transitions.
- Changing the `409` body shape or the re-index rule for a forbidden pair.
- The title, priority, and timestamp work in `add-ticket-persistence`.

## Decisions

### One class throws, and the screen asks it for targets

`tickets.domain.TicketStateMachine` holds the five pairs. `require(from, to)` throws `InvalidTransitionException` when the pair is absent. `allowedTargets(from)` returns only the statuses reachable from `from` by those pairs, so the edit screen does not probe by catching exceptions.

The exception is unchecked and lives in `tickets.domain`. Its message is `Status cannot change from {from} to {to}`. `TicketService` catches it and keeps the current `409` mapping. A thrown pair does not update the row and does not re-index.

Alternative: keep `StatusPolicy.allowed` and throw only in the service. Rejected because the request is a domain class that throws, with no Spring or database types on it.

Alternative: only the throwing method. Rejected because the edit screen would catch `InvalidTransitionException` once per status.

### `StatusPolicy` is removed

Callers use `TicketStateMachine`. Two classes would let a pair be allowed in one and rejected in the other.

If `add-ticket-persistence` has already replaced `Status` with `TicketStatus`, this class uses `TicketStatus`. Otherwise it uses `Status`. It does not introduce a second status enum.

### Tests stay on the pairs

The 25 status pairs stay as unit tests. The five removed allows assert that `require` throws. The five remaining allows assert that it returns without throwing. API coverage keeps one allowed patch and adds `OPEN` to `RESOLVED` as `409` with the stored status unchanged.

## Risks / Trade-offs

- [Clients that resolve an `OPEN` ticket, send work back, or reopen `RESOLVED`, `CLOSED`, or `CANCELLED` now get `409`] → The message names both statuses. No data rewrite. Tickets already in `CLOSED` or `CANCELLED` stay there.
- [The edit control for `OPEN` currently can offer `RESOLVED`] → It offers `IN_PROGRESS` and `CANCELLED` only, plus the current status.
- [`add-ticket-persistence` and this change both touch `tickets.domain`] → This change does not edit title or priority. Whichever change applies second keeps a single status enum.

## Migration Plan

1. Update `spec/state-machine.md` to the five allowed pairs.
2. Replace `StatusPolicy` with `TicketStateMachine` and point the service and edit screen at it.
3. Roll back by restoring `StatusPolicy` and the previous matrix. Stored statuses do not need a migration.

## Open Questions

None.
