# UI flow

The UI is four surfaces on four routes. The assistant is a panel on every route. It never creates or edits a ticket.

| Route | Screen |
|---|---|
| `/tickets` | Ticket list, with the assistant panel |
| `/tickets/new` | Create form, with the assistant panel |
| `/tickets/{id}` | Ticket detail, with the assistant panel |
| `/tickets/{id}/edit` | Edit form, with the assistant panel |

| Assistant placement | What the user can see at once | Result |
|---|---|---|
| Panel on the ticket screens | The ticket and the answer | Use this |
| Separate `/assistant` page | The answer alone | The source link is the only way back to the ticket |

**Trade-offs.** A panel keeps a source ticket one click away from the answer. It uses space on the create form even when the user is not asking. A separate page would hide that distraction and would hide the ticket the answer cites.

## Ticket list

`/tickets` loads `GET /api/tickets` with `page` defaulting to `0` and `size` fixed at `20`. The user does not type `page` or `size`.

The screen shows a keyword box, a status select, a Search button, a Create ticket link to `/tickets/new`, and one row per `content` item. A row shows `id`, `description`, `status`, `priority`, `category`, and the assignee. A null assignee is shown as `Unassigned`. Choosing a row opens `/tickets/{id}`.

The status select is `All`, `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, or `CANCELLED`. `All` omits `status`. Search sends the trimmed keyword. An empty keyword omits `keyword`. Previous and Next change `page` by one. Previous is disabled on page `0`. Next is disabled when `(page + 1) * size` is greater than or equal to `total`. The label is `Page {page + 1}`. An empty `content` array shows `No tickets match.` and no rows.

| Search | When the request runs | Result |
|---|---|---|
| Search button | After the user commits the keyword and status | Use this |
| Every keystroke | A request per character | Partial words flash empty pages |

**Trade-offs.** Search-on-submit matches the list endpoint and keeps a blank box from filtering. Typing does not show results until Search. `Unassigned` fills an otherwise empty cell. The raw null is what the API returns and is not what the row shows.

## Ticket detail

`/tickets/{id}` loads `GET /api/tickets/{id}` and shows `id`, `description`, `status`, `priority`, `category`, assignee or `Unassigned`, and resolution notes or `None`. Comments are in response order. Each comment shows `body` and `createdAt`. Edit links to `/tickets/{id}/edit`.

Under the comments, a box and an Add comment button call `POST /api/tickets/{id}/comments`. Success appends the returned comment. The box is cleared. The rest of the ticket stays as loaded.

## Create and edit form

One form serves create and edit. Create does not show status, id, or resolution notes. Edit does not change description, priority, or category. Those three stay visible and read-only because `PATCH` does not accept them.

| Mode | Route | Fields the user can change | Submit |
|---|---|---|---|
| Create | `/tickets/new` | `description`, `priority`, `category`, `assignee` | `POST /api/tickets` |
| Edit | `/tickets/{id}/edit` | `status`, `assignee`, `resolutionNotes` | `PATCH /api/tickets/{id}` |

Create sends `assignee` only when the trimmed value is non-empty. Priority is a select of `HIGH`, `MEDIUM`, and `LOW`. The request sends that uppercase value. `category` stays free text. Success `201` opens `/tickets/{id}` for the returned id.

Edit loads the ticket first. The status select shows the stored status, labeled `Current`, and only the targets `spec/state-machine.md` marks Allowed. Submit omits `status` when Current is still selected. A non-empty assignee sends that string. An assignee box that the user emptied sends `null`. Whitespace does not submit. Resolution notes omitted means the box was left as loaded. A non-empty change sends the trimmed text. A Clear resolution notes control sends `null`. Success `200` opens the detail for the returned ticket.

| Status control | What can be submitted | Result |
|---|---|---|
| Current status plus allowed targets | A forbidden pair is not in the list | Use this. A stale ticket can still return `409` |
| All five statuses | The user can request a forbidden pair | The only explanation is the `409` banner |

**Trade-offs.** Hiding forbidden targets stops a no-change submit, which the API rejects with `409`. The list can be wrong if the ticket changed after the form loaded, so the banner still has to show that `409`. Create never sends `status`. The server assigns `OPEN` and the id.

## Assistant panel

The panel has a question box and an Ask button. Ask calls `POST /api/ai/ask` and does not send ticket fields. The button is disabled while the request runs.

A `200` with `noRelevantTickets` false shows `answer` verbatim, then one source row per item, in response order. A source row shows `ticketId`, `title`, and `similarity` as returned. The ticket id links to `/tickets/{ticketId}`. A `200` with `noRelevantTickets` true shows `answer` and no source list. The no-match answer is the server string, including the period. It is not shown as an error.

Asking does not change the list, the detail, or the form. A question is not prefilled from the open ticket.

## User flows

Search and open. On `/tickets`, enter `export`, choose `OPEN`, and press Search. The list requests `keyword=export` and `status=OPEN`. Open `TKT-1001`. The detail shows the description, the comment, and `None` for resolution notes.

Create. Open `/tickets/new`, enter description, priority, and category, leave assignee empty, and submit. The browser opens `/tickets/TKT-1001` after `201`. A blank description does not call the API. The description field shows `must not be blank`.

Edit status. On `/tickets/TKT-1002/edit`, choose `CLOSED` only when the loaded status is `RESOLVED`, because that is the allowed close. Submit. Detail shows `CLOSED` and the same resolution notes. Choosing Current and changing nothing else does not call the API. The banner shows `must change at least one field`.

Comment. On the detail, submit `Tried CSV and PDF.` The new comment appears under the existing ones. A blank box does not call the API and shows `must not be blank` on the comment field.

Ask. In the panel, ask `Why do quarterly invoice exports fail?` The panel shows the answer and a source link for each returned ticket. Follow `TKT-1001` to the detail. Ask `How do I reset a VPN password?` The panel shows exactly `No relevant tickets found.` and no source list. The open ticket is unchanged.

## Field validation

The client checks on submit and uses the server message text. A failing check does not call the API. Editing the field clears that field's message.

| Field | Screen | When | Message |
|---|---|---|---|
| `description` | Create | Trimmed value is empty | `must not be blank` |
| `priority` | Create | The select is empty | `must not be blank` |
| `priority` | Create | The value is not `HIGH`, `MEDIUM`, or `LOW` | `must be HIGH, MEDIUM, or LOW` |
| `category` | Create | Trimmed value is empty | `must not be blank` |
| `assignee` | Create or edit | The box is only whitespace | `must not be blank` |
| `resolutionNotes` | Edit | The box is only whitespace | `must not be blank` |
| `body` | Detail comment | Trimmed value is empty | `must not be blank` |
| `question` | Assistant panel | Trimmed value is empty | `must not be blank` |
| Form | Edit | Status is Current, assignee is unchanged, and resolution notes were not cleared or replaced | `must change at least one field` |

An empty assignee on create is valid and is omitted. An emptied assignee on edit is valid and sends `null`.

## Backend errors

One banner at the top of the current screen shows errors that are not tied to a visible field. A new response replaces the banner. `400` entries in `fieldErrors` are shown under the matching field, using `fieldErrors[].message` unchanged. They are not repeated in the banner.

| Response | Where it shows | Text |
|---|---|---|
| `400` and the field is on the screen | Under that field | `fieldErrors[].message` |
| `400` for `id`, `page`, `size`, or list `status` | Banner | That entry's `message` |
| `404` | Banner on the detail route. No ticket fields are shown | `message`, for example `Ticket TKT-1001 not found` |
| `409` | Banner on the edit form. Values stay as submitted | `message`, for example `Status cannot change from OPEN to OPEN` |
| `503` after a ticket write | Banner on the form or detail. Entered values stay | `Ticket could not be indexed` |
| `503` after Ask | Banner in the panel. The previous answer stays | `Question could not be answered` |
| Body is not the shared error shape, or the network fails | Banner | `The server could not be reached.` |

A no-match `200` never uses the banner. Save, Add comment, and Ask stay disabled until the response arrives.

| `400` display | What the user reads | Result |
|---|---|---|
| Server message under the field | The same words as `fieldErrors` | Use this |
| A rewritten hint in the banner | A second wording for one failure | The field and the banner can disagree |

**Trade-offs.** Reusing `fieldErrors[].message` keeps the screen and the API on one string. The client message for a blank field appears before the request, and the server message replaces it when the request still fails. `409` and `503` have no field, so they use the banner and leave the form filled in for a retry. The no-match sentence stays in the answer area so a refusal is not styled as a broken request.
