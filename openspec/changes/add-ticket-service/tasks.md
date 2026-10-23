# Tasks

## 1. Request records

- [ ] 1.1 Confirm `tickets.domain.TicketStateMachine` and a stored ticket `title` already exist. If either is missing, stop and apply `add-ticket-persistence` and `add-ticket-state-machine` first. Verify `./mvnw -q -DskipTests compile` from `backend/` exits 0 before any service edit.
- [ ] 1.2 Add `spring-boot-starter-validation`. Add controller request records that trim text, uppercase priority, and use the spec messages for title length, description, comment `body`, and `@Null` `status` on create and general update. Map `MethodArgumentNotValidException` through the existing advice to one `fieldErrors` entry per field. Verify an API test that title `Tiny` returns `400`, field `title`, message `must be between 5 and 120 characters`, and a single entry for that field.

## 2. Ticket service

- [ ] 2.1 Implement create, get, list, and general update on `TicketService`. Create requires a valid title, description, priority, and category. General update changes only the present title, description, priority, assignee, and resolution notes. Verify API tests for a valid create storing title `Export timeout`, a 5001-character description rejected with `must be at most 5000 characters`, a comment of 2001 characters rejected with `must be at most 2000 characters`, a title patch to `Export timeout`, and a general patch that includes `status` returning `400` with message `must not be set` while the stored status stays unchanged.
- [ ] 2.2 Add `PATCH /api/tickets/{id}/status` and call `TicketStateMachine.require`. Map `InvalidTransitionException` to `409` with its message, an empty `fieldErrors`, no row change, and no re-index. Verify `OPEN` to `IN_PROGRESS` returns `200` on that path, and `OPEN` to `OPEN` returns `409` with message `Status cannot change from OPEN to OPEN`.
- [ ] 2.3 Limit keyword search to a case-insensitive substring of title and description, still AND-ed with the status filter. Update `spec/api-contract.md` for the status route, the length rules, the general update fields, and the narrower keyword. Verify an API test that `keyword=payroll` matches a title and does not match a comment-only hit, and verify `spec/api-contract.md` says keyword matches title and description only.

## 3. Screens

- [ ] 3.1 Block title `Tiny` on the create page before the request, and show `must be between 5 and 120 characters`. Send an edit status change only to `PATCH /api/tickets/{id}/status`. Update `spec/ui-flow.md` for that path and the title message. Verify a slice test that the create page contains the length check and the edit page contains `/api/tickets/` and `/status` without putting `status` on the general update.

## Workflow follow-up

- Archive `add-ticket-persistence` and `add-ticket-state-machine` before this change, then reconcile this delta with the updated `tickets` spec.
- Archive this change with `/opsx-archive` after that reconciliation and review.
