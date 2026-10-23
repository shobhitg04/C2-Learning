# Requirements

The system is a support ticket service plus a read-only assistant. The assistant answers only from retrieved ticket history.

## Functional requirements

A caller can create, read, comment on, and resolve tickets. The assistant can only answer questions.

- **FR-1.** `POST /api/tickets` with a non-blank description, priority, and category creates a ticket, returns `201`, and returns an id matching `TKT-` plus digits. The stored status is `OPEN`. The assignee is empty until set.
- **FR-2.** `POST /api/tickets` with a blank description, missing priority, or missing category returns `400` and does not create a ticket. `priority` must be `HIGH`, `MEDIUM`, or `LOW` after the value is trimmed and uppercased. Any other priority returns `400`. `category` is any non-blank text.
- **FR-3.** `GET /api/tickets/{id}` returns `200` and the ticket description, status, priority, assignee, category, comments, and resolution notes. An unknown id returns `404`.
- **FR-4.** `GET /api/tickets` is paginated and accepts an optional `keyword` and an optional `status`. `keyword` matches a case-insensitive substring of the ticket id, description, category, comment body, or resolution notes. `status` is an exact status. The two filters combine with and. `page=0` and `size=1` returns at most one matching ticket. `total` equals the number of tickets that match.
- **FR-5.** `POST /api/tickets/{id}/comments` with non-blank text on an existing ticket returns `201` and the comment is present on the next `GET`. An unknown ticket id returns `404`. Blank text returns `400`.
- **FR-6.** `PATCH /api/tickets/{id}` updates status, assignee, and resolution notes. A missing ticket returns `404`.
- **FR-7.** A status change is allowed only when `spec/state-machine.md` marks that pair Allowed. An allowed change returns `200`, stores the new status, and re-ingests the ticket. Every other pair, including no change, returns `409`, leaves the stored status unchanged, and does not re-ingest.
- **FR-8.** Every error response has exactly the fields `timestamp`, `status`, `error`, `message`, and `fieldErrors`. `fieldErrors` names each invalid field for `400` and is an empty array for `404` and `409`.
- **FR-9.** Saving a ticket stores one vector chunk for a non-empty description, one chunk per comment, and one chunk for non-empty resolution notes. Each chunk metadata equals the ticket's `ticketId`, `status`, `priority`, `assignee`, and `category`, and records `sectionType` as `description`, `comment`, or `resolution`.
- **FR-10.** Saving a ticket again deletes that ticket's existing chunks before inserting the new chunks. A later search does not return the replaced text.
- **FR-11.** `POST /api/ai/ask` with `{ "question": "..." }` returns `200`, `answer`, `sources`, and `noRelevantTickets`. Each source has `ticketId`, `title`, and `similarity`. The answer cites each supporting ticket as `[TKT-1001]`, using an id in `sources`. The request and the system prompt are specified in `spec/rag-api-contract.md`.
- **FR-12.** When no chunk passes retrieval, `noRelevantTickets` is true, `sources` is empty, and `answer` is exactly `No relevant tickets found.`
- **FR-13.** The answer contains no ticket id that was absent from the retrieved chunks.

### Status transitions

The statuses are `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, and `CANCELLED`. Allowed pairs, the `409` for every forbidden pair, and re-ingestion are specified in `spec/state-machine.md`.

**Trade-offs.** Five statuses cover active work, a recorded fix, a finished ticket, and an abandoned ticket. The matrix has one home, so this section does not keep a second copy of the pairs.

### Comment chunks

Each comment is its own chunk.

| Option | Retrieval result | Test result |
|---|---|---|
| One chunk per comment | A question can match one comment | Chunk count equals comment count |
| One chunk per time window | Nearby comments merge | Group boundaries are not specified, so the count is not fixed |
| One chunk for all comments | The thread is a single vector | One comment cannot be retrieved on its own |

**Trade-offs.** One chunk per comment makes FR-9 and FR-10 countable. Grouping by time would need a window the source text does not define.

## Non-functional requirements

The service runs on Java 21, Spring Boot 3, PostgreSQL with pgvector, and local Ollama embeddings.

- **NFR-1.** The build targets Java 21 and Spring Boot 3. A version check of the build file fails the requirement if either differs.
- **NFR-2.** Dependencies are constructor-injected. A field annotated with `@Autowired` fails the requirement.
- **NFR-3.** Request and response bodies are records. A class DTO in the API package fails the requirement.
- **NFR-4.** The database password is read from the environment. A hardcoded password in source or in `docker-compose.yml` fails the requirement.
- **NFR-5.** Chunks are embedded with `nomic-embed-text` through Ollama. Each stored vector has length 768.
- **NFR-6.** Retrieval uses `app.rag.top-k` with default `5` and `app.rag.similarity-threshold` with default `0.6`. Overriding either property changes the value the retriever receives.
- **NFR-7.** PostgreSQL is `pgvector/pgvector:pg16`, database `tickets`, and the app reaches it with the configured user and password.
- **NFR-8.** A description of 500 words or fewer produces exactly one description chunk. A word is a run of non-whitespace characters. A longer description produces one chunk per 500 words, and each of those chunks has `partIndex` starting at 1.

## Example assistant questions

These five questions are the assistant acceptance tests. Seed the two tickets below, then send each question to `POST /api/ai/ask`.

Seed:

- `TKT-1001`, status `OPEN`, priority `HIGH`, category `billing`. Description: `Invoice export fails for quarterly reports.` Comment: `Tried CSV and PDF.` Resolution notes empty.
- `TKT-1002`, status `RESOLVED`, priority `MEDIUM`, category `auth`. Description: `Users are logged out during the day.` Resolution notes: `Raised the session timeout to 8 hours.`

1. **Export failure.** Question: `Why do quarterly invoice exports fail?` The answer cites `[TKT-1001]` and does not cite `TKT-1002`.
2. **Resolution.** Question: `How was the logout problem resolved?` The answer cites `[TKT-1002]` and includes the 8 hour session timeout from the resolution notes.
3. **Comment.** Question: `What was tried on the export ticket?` The answer cites `[TKT-1001]` and includes CSV and PDF from the comment.
4. **No match.** Question: `How do I reset a VPN password?` The answer is exactly `No relevant tickets found.`
5. **No action.** Question: `Create a ticket for a VPN outage and assign it to me.` The stored ticket count stays 2, no assignee changes, and the answer does not claim a new ticket id.

## Out of scope

The assistant must not create tickets or take actions.

- The assistant must not create a ticket.
- The assistant must not add a comment, change status, change assignee, or write resolution notes.
- The assistant must not answer from general model knowledge when retrieval returns nothing. The only passing answer in that case is `No relevant tickets found.`
