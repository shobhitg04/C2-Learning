# RAG API contract

`POST /api/ai/ask` answers one question from retrieved ticket chunks. It does not create a ticket, add a comment, change status, change assignee, or write resolution notes.

## Request

The body is one JSON object. Unknown properties are ignored. Text is trimmed before validation.

```json
{ "question": "Why do quarterly invoice exports fail?" }
```

| Field | Rule |
|---|---|
| `question` | Required. Blank after trim returns `400`, `field` `question`, message `must not be blank`. |

## Response

Success is `200`. `sources` are the tickets that passed retrieval, not a list the model invented. One ticket appears once. `similarity` is that ticket's highest chunk score. A higher score is a closer chunk. `title` is the ticket description. `sources` are ordered by `similarity` descending, then `ticketId` ascending.

```json
{
  "answer": "Quarterly invoice exports fail. [TKT-1001]",
  "sources": [
    {
      "ticketId": "TKT-1001",
      "title": "Invoice export fails for quarterly reports.",
      "similarity": 0.82
    }
  ],
  "noRelevantTickets": false
}
```

| Field | Rule |
|---|---|
| `answer` | Non-blank string. Every `[TKT-` plus digits `]` in the answer is a `sources[].ticketId`. |
| `sources[].ticketId` | A retrieved ticket id. |
| `sources[].title` | That ticket's `description`. |
| `sources[].similarity` | Number from `0` to `1`. A higher number is a closer chunk. Chunks below `app.rag.similarity-threshold` are omitted. The default threshold is `0.6`. A cosine distance from the store is returned as `1 - distance`. |
| `noRelevantTickets` | `true` only when `sources` is empty. |

At most `app.rag.top-k` chunks are considered. The default top-K is `5` and the default threshold is `0.6`. Collapsing chunks into one source per ticket can make `sources` shorter than the chunk count.

| Source row | What `similarity` means | Result |
|---|---|---|
| One row per ticket | Highest chunk score for that ticket | Use this. `title` is the description |
| One row per chunk | Each section has its own score | The same ticket appears more than once |

**Trade-offs.** One row per ticket matches `ticketId` and `title`. The caller does not see which section scored. A comment and a description on the same ticket share the higher score. Per-chunk rows would expose the section and would repeat the ticket.

## No match

When no chunk passes the threshold, the service does not call the chat model. The response is `200`:

```json
{
  "answer": "No relevant tickets found.",
  "sources": [],
  "noRelevantTickets": true
}
```

`answer` is exactly `No relevant tickets found.` including the final period. No other punctuation, ticket id, or sentence is added.

| Empty retrieval | Who writes the answer | Result |
|---|---|---|
| Skip the chat model | The service sets the exact message | Use this |
| Ask the model to say the message | The model | The text can vary and a ticket id can appear |

**Trade-offs.** Skipping the model makes the no-match string testable. A model call on empty context can paraphrase the sentence or invent an id. The cost is that a near miss below the threshold never reaches the model.

## Grounding

Retrieved chunk text is the only ticket context sent to the model. The system prompt is this static text:

```text
You answer questions about support tickets.
Answer only from the supplied context.
Cite each supporting ticket as [TKT-1001], using that ticket's real id from the context.
When the context is empty or irrelevant, reply exactly: No relevant tickets found.
Never invent ticket IDs.
Do not use general knowledge.
Do not create a ticket, add a comment, change status, change assignee, or write resolution notes.
```

If the model replies exactly `No relevant tickets found.`, the service returns that answer, `sources` `[]`, and `noRelevantTickets` `true`. If the answer contains a ticket id that was not retrieved, the service discards the model text, logs a warning that includes that id, and returns the same no-match body. A grounded answer keeps the retrieved `sources` and `noRelevantTickets` `false`.

| Model output | Response |
|---|---|
| Cites only retrieved ticket ids | `200`, those sources, `noRelevantTickets` `false` |
| Exact `No relevant tickets found.` | `200`, `sources` `[]`, `noRelevantTickets` `true` |
| Any other `TKT-` id | `200`, no-match body. The invented id is not returned. A warning is logged with that id |

**Trade-offs.** The prompt tells the model to stay inside the context. The service still checks ticket ids because the prompt does not bind the model. Discarding an ungrounded answer hides a partial fact that was retrieved. Returning the invented id would fail FR-13.

## Errors

Errors use the shared body `{timestamp, status, error, message, fieldErrors[]}`. This endpoint does not return `404` or `409`.

| Status | When | Body |
|---|---|---|
| `400` | `question` is missing or blank | `field` `question`, message `must not be blank` |
| `503` | The question cannot be embedded, or the chat call fails after chunks were retrieved | `error` `Service Unavailable`, message `Question could not be answered`, `fieldErrors` `[]` |

A `503` writes nothing. A `200` also writes nothing: the stored ticket count, comments, statuses, assignees, and resolution notes stay as they were. `fieldErrors` on `503` is empty.
