# Ticket API contract

The ticket API is JSON over HTTP. `POST /api/ai/ask` is specified in `spec/rag-api-contract.md` and is not a ticket resource.

| Method | Path | Success |
|---|---|---|
| `POST` | `/api/tickets` | `201` |
| `GET` | `/api/tickets` | `200` |
| `GET` | `/api/tickets/{id}` | `200` |
| `PATCH` | `/api/tickets/{id}` | `200` |
| `POST` | `/api/tickets/{id}/comments` | `201` |

Unknown JSON properties are ignored. The server trims text before it validates. Timestamps are UTC ISO-8601.

## Shared error body

Every `400`, `404`, and `409` uses one body. `fieldErrors` names each invalid field on `400` and is an empty array on `404` and `409`.

```json
{
  "timestamp": "2026-10-23T14:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": [
    { "field": "description", "message": "must not be blank" }
  ]
}
```

| Status | `error` | When | `fieldErrors` |
|---|---|---|---|
| `400` | `Bad Request` | A field, query parameter, or body fails validation | One entry per invalid field |
| `404` | `Not Found` | No ticket has that id | `[]` |
| `409` | `Conflict` | The status change is not an allowed transition, including no change | `[]` |

`404` message is `Ticket TKT-1001 not found`, using the requested id. `409` message is `Status cannot change from OPEN to OPEN`, using the stored status and the requested status.

## Ticket detail

`GET /api/tickets/{id}`, `POST /api/tickets`, and `PATCH /api/tickets/{id}` return this object. Comments are ordered by `createdAt`, then `id`. `resolutionNotes` is null when the ticket has no resolution row.

```json
{
  "id": "TKT-1001",
  "description": "Invoice export fails for quarterly reports.",
  "status": "OPEN",
  "priority": "HIGH",
  "assignee": null,
  "category": "billing",
  "comments": [
    {
      "id": 1,
      "body": "Tried CSV and PDF.",
      "createdAt": "2026-10-23T14:00:00Z"
    }
  ],
  "resolutionNotes": null
}
```

## Create a ticket

`POST /api/tickets` creates an `OPEN` ticket. The server assigns `id`. On an empty database the first id is `TKT-1001`, and each later ticket uses the next integer. The client does not send `id` or `status`.

```json
{
  "description": "Invoice export fails for quarterly reports.",
  "priority": "HIGH",
  "category": "billing",
  "assignee": null
}
```

| Field | Rule |
|---|---|
| `description` | Required. Blank after trim returns `400`, `field` `description`, message `must not be blank`. |
| `priority` | Required. After trim and uppercase, `HIGH`, `MEDIUM`, or `LOW`. Anything else returns `400`, `field` `priority`, message `must be HIGH, MEDIUM, or LOW`. |
| `category` | Required. Blank after trim returns `400`, message `must not be blank`. |
| `assignee` | Optional. Omitted or null stores null. Blank after trim returns `400`, message `must not be blank`. |
| `status` | Rejected. A present `status` returns `400`, message `must not be set`. |

Success is `201` and the ticket detail. `comments` is `[]` and `resolutionNotes` is null. No ticket is stored when validation fails.

## List, search, and filter

`GET /api/tickets` returns one page. `keyword` and `status` are optional and combine with AND. Results are ordered by the numeric part of `id`, ascending.

`GET /api/tickets?page=0&size=1&keyword=export&status=OPEN`

| Parameter | Rule |
|---|---|
| `page` | Default `0`. An integer less than `0` returns `400`, message `must be zero or greater`. |
| `size` | Default `20`. Outside `1` through `100` returns `400`, message `must be between 1 and 100`. |
| `keyword` | Omitted or blank means no keyword filter. Otherwise the trimmed value is a case-insensitive substring of the ticket `id`, `description`, `category`, a comment `body`, or resolution `notes`. `%` and `_` are literal characters. |
| `status` | Omitted means every status. `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, or `CANCELLED`. Any other value returns `400`, `field` `status`, message `must be OPEN, IN_PROGRESS, RESOLVED, CLOSED, or CANCELLED`. |

```json
{
  "content": [
    {
      "id": "TKT-1001",
      "description": "Invoice export fails for quarterly reports.",
      "status": "OPEN",
      "priority": "HIGH",
      "assignee": null,
      "category": "billing"
    }
  ],
  "page": 0,
  "size": 1,
  "total": 1
}
```

`total` is the count after `keyword` and `status`. With no filters it equals the number of stored tickets. The page omits comments and resolution notes. Keyword search still reads comment bodies and resolution notes, and it also matches `id` and `category`.

A `page` past the last page returns `200`. `content` is `[]`. `page` and `size` are the requested values. `total` stays the match count. When `total` is `0`, the only in-range page is `0`. When `total` is greater than `0`, the last page index is `(total - 1) / size` using integer division. A greater `page` is past the end.

| List shape | Payload | Keyword behavior |
|---|---|---|
| Summary page | Identity and ticket fields only | `keyword` still matches id, category, comments, and resolution notes |
| Full ticket on every row | Comments repeated on each page | Same match, larger response |

**Trade-offs.** Query parameters keep search, status, and pagination on the list endpoint. A separate search route would duplicate paging. The summary page stays small. `GET /api/tickets/{id}` returns comments and resolution notes. A blank `keyword` is ignored so an empty search box does not return `400`. Matching `id` and `category` as well as the free text lets a caller find `TKT-1001` or `billing` without a second filter.

## Get a ticket

`GET /api/tickets/{id}` returns `200` and the ticket detail. An id that matches no row returns `404`. An id that does not match `TKT-` plus digits returns `400`, `field` `id`, message `must match TKT- and digits`.

## Update a ticket

`PATCH /api/tickets/{id}` changes only the fields that are present. Omitted fields stay as stored. An unknown id returns `404`.

```json
{
  "status": "RESOLVED",
  "assignee": "sam",
  "resolutionNotes": "Raised the session timeout to 8 hours."
}
```

| Field | Rule |
|---|---|
| `status` | Optional. One of the five statuses, otherwise `400`. A pair `spec/state-machine.md` marks Forbidden, including no change, returns `409` and does not change the row or the vector chunks. |
| `assignee` | Optional. A string stores that assignee. Null clears it. Blank after trim returns `400`, message `must not be blank`. |
| `resolutionNotes` | Optional. A non-blank string replaces the resolution row. Null deletes the resolution row. Blank after trim returns `400`, message `must not be blank`. |

An empty object returns `400`, `field` `body`, message `must change at least one field`. Success is `200` and the ticket detail. Allowed status pairs, and the re-ingestion that follows each one, are in `spec/state-machine.md`.

## Add a comment

`POST /api/tickets/{id}/comments` adds one comment. An unknown ticket returns `404`.

```json
{ "body": "Tried CSV and PDF." }
```

`body` is required. Blank after trim returns `400`, `field` `body`, message `must not be blank`. Success is `201`:

```json
{
  "id": 1,
  "body": "Tried CSV and PDF.",
  "createdAt": "2026-10-23T14:00:00Z"
}
```

The next `GET /api/tickets/{id}` includes that comment.

## Ingestion failure

A create, update, comment, or allowed status change, including close, indexes the ticket in the same transaction. If indexing fails, the ticket write rolls back and the response is `503` with the shared error body. `fieldErrors` is `[]`. `message` is `Ticket could not be indexed`. The previous chunks for that ticket remain. Details are in `spec/rag-ingestion.md`.
