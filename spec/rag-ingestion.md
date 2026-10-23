# RAG ingestion

`TicketIndexer` turns one ticket into section documents and stores them in `vector_store`. The assistant never calls the indexer. A ticket and its chunks commit together.

## From ticket to documents

The indexer reads the ticket row, its comments ordered by `created_at` then `id`, and its resolution row if one exists. It writes one document per non-empty section.

| Section | Source | Chunk count |
|---|---|---|
| Description | `ticket.description` | One when the text is 500 words or fewer. A word is a run of non-whitespace characters. Longer text is one chunk per 500 words, with `partIndex` starting at 1. A single chunk has no `partIndex`. |
| Comment | `comment.body` | One per comment. `groupIndex` is `1` for the earliest comment. |
| Resolution | `ticket_resolution.notes` | One when the row exists. Zero when it does not. |

`TKT-1001` in the requirements seed is two documents: the description and comment 1. `TKT-1002` is two documents: the description and the resolution notes.

| Split | What the indexer would emit | Result |
|---|---|---|
| Section | One document per description, comment, and resolution | Use this |
| Whole ticket | One document for every field | A question about the fix retrieves the complaint with equal weight |
| Paragraph inside a section | Extra documents where the author left a blank line | Boundaries follow typing, not the ticket fields |

**Trade-offs.** Section documents keep a comment and the resolution searchable on their own. They cost one embedding call per section on every re-ingestion. A description of 500 words or fewer is not split. Past that, parts of 500 words keep each embedding inside a bounded size. Paragraph and whole-ticket splits are the ones the architecture already rejected.

## Chunk format

`vector_store.content` is plain text. The first lines are the ticket id and the section name, then the section body. The embedding is computed on this whole text.

Description:

```text
Ticket: TKT-1001
Section: description
Invoice export fails for quarterly reports.
```

Comment:

```text
Ticket: TKT-1001
Section: comment
Index: 1
Tried CSV and PDF.
```

Resolution:

```text
Ticket: TKT-1002
Section: resolution
Raised the session timeout to 8 hours.
```

The comment document is the only one with an `Index` line. That number equals `groupIndex`.

## Metadata

Every document stores the same metadata keys. Values are copied from the ticket at index time. `sectionType` is `description`, `comment`, or `resolution`. `groupIndex` is present only on comment documents.

| Field | Example for the `TKT-1001` description |
|---|---|
| `ticketId` | `TKT-1001` |
| `status` | `OPEN` |
| `priority` | `HIGH` |
| `assignee` | `null` |
| `category` | `billing` |
| `sectionType` | `description` |

The embedding column is `vector(768)` from Ollama `nomic-embed-text`. Document `id` is a new UUID on each insert.

## When ingestion runs

The indexer runs after the ticket rows are written and before the transaction commits. Close is the allowed move `RESOLVED` to `CLOSED`. It uses the same path as any other allowed status change. Chunks stay in the index with `status` `CLOSED`.

| Trigger | Runs | Does not run |
|---|---|---|
| Create `POST /api/tickets` | Yes. One ingest of the new `OPEN` ticket. | |
| Update `PATCH /api/tickets/{id}` that changes assignee or resolution notes | Yes. One ingest after the row change. | |
| Comment added `POST /api/tickets/{id}/comments` | Yes. One ingest of the whole ticket, not an append of one chunk. | |
| Allowed status change, including close | Yes. Metadata `status` matches the new status. | |
| Forbidden status change | | `409`. Rows and chunks stay as they were. |
| Validation failure | | `400`. Nothing is written. |
| Assistant question | | The assistant only reads `vector_store`. |

| Closed tickets | Search result | Result |
|---|---|---|
| Re-ingest and keep chunks | A later question can still cite the resolution | Closed tickets compete for top-K |
| Delete chunks on close | The index holds only active work | A question about a finished fix has no context |

**Trade-offs.** Re-ingesting the whole ticket on a comment or an assignee change rewrites metadata that is copied onto every chunk. Appending one comment chunk would leave the older chunks with a stale status after the next transition. Keeping closed tickets searchable matches the resolution questions in the requirements. Removing them on close would make those answers fail after `RESOLVED` to `CLOSED`.

## Delete-then-insert

Re-ingestion replaces every chunk for one `ticketId`. It does not update rows in place.

1. Delete `vector_store` rows whose `metadata->>'ticketId'` equals this ticket.
2. Insert the documents built from the current rows, each with a new UUID and a new embedding.
3. Commit the ticket write and the vector writes together.

A second save of the same text does not return the replaced content. The old UUIDs are gone. An unchanged section is embedded again because the replacement is the whole ticket, not a diff.

| Strategy | After a status change | Result |
|---|---|---|
| Delete then insert | Old chunks are gone before the new set is visible | Use this. Metadata cannot lag behind the row |
| Update metadata in place | Embeddings of unchanged text are kept | A missed row keeps the old status |
| Append only | New comments add rows | Stale chunks remain beside the new ones |

**Trade-offs.** Delete-then-insert spends an embedding call on every section, including text that did not change. It makes FR-10 observable: a search after the save does not return the replaced text. In-place metadata updates would avoid those calls and would need a second path for body changes. The delete and the insert share the ticket transaction, so a failed insert does not leave the ticket with zero chunks.

## Failure handling

Indexing is in the ticket transaction. If Ollama or the vector insert fails, the transaction rolls back. The client receives `503` and the shared error body. `message` is `Ticket could not be indexed`. `fieldErrors` is `[]`. The ticket row and the previous chunks are unchanged.

| Failure point | What commits | What the client sees |
|---|---|---|
| Embed or insert fails inside the ticket transaction | Nothing for that request. Previous chunks remain | `503`. Retrying the same write indexes the ticket |
| Ticket commits, index runs later | The ticket, with stale or missing chunks | `200` or `201` while search is wrong |
| Delete commits before embed | A ticket with no chunks | Search misses the ticket until a later repair |

**Trade-offs.** Rolling back the ticket write keeps the row and the index on the same text. The client must retry the create, update, comment, or status change. A saved ticket with a background index would return success while questions still see the old chunks, and it would need a retry queue this service does not have.

## Backfill

Backfill runs the same delete-then-insert for tickets that already exist, including tickets that have no chunks. It is not an assistant action and it is not a ticket endpoint.

`app.rag.backfill` defaults to `false`. When it is `true`, startup loads every `ticket.id` ordered by the numeric part of the id and reindexes one ticket at a time. Each ticket uses its own transaction. Startup then serves traffic with the flag still `true` only for that process. The operator turns the flag off after the recovery boot so the next start does not embed every ticket again.

| Ticket outcome | Transaction | Next ticket |
|---|---|---|
| Documents match the current rows | Commits | Continues |
| Embed or insert fails | Rolls back. Existing chunks for that ticket stay | Continues |

The run result is the number of tickets indexed and the ids that failed. A second backfill of an unchanged ticket replaces chunks with the same text and new UUIDs. Search still returns that ticket. One failure does not stop the rest and does not roll back tickets that already committed.

| Trigger | When it embeds | Result |
|---|---|---|
| Flag off | Never at startup | Normal boots stay cheap |
| Flag on, one ticket per transaction | Every stored ticket on that boot | Use this for recovery |
| Always on startup | Every boot | Repeats a full embed with no new ticket text |

**Trade-offs.** A startup flag reuses `TicketIndexer` and stays off the public API, so the assistant still cannot write. It will re-embed every ticket on a boot that leaves the flag on. An admin HTTP route would need an authorization rule this service does not define. Per-ticket transactions keep a failure on `TKT-1002` from undoing `TKT-1001`.
