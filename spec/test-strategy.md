# Test strategy

Tests are JUnit 5 and AssertJ. A test name is `should_expectedBehaviour_when_condition`. The class under test is never mocked. `@SpringBootTest` is only for integration and API tests. The default run does not call Ollama.

| Layer | Boots Spring | Proves | Does not prove |
|---|---|---|---|
| Unit | No | Pure rules: chunks, citations, the transition decision | HTTP or Postgres |
| Integration | Yes | Repositories and `vector_store` on `pgvector/pgvector:pg16` via Testcontainers | The full HTTP contract |
| API | Yes | Status codes, the error body, and one path through each endpoint | All 25 transitions again |

| Database in tests | What it can store | Result |
|---|---|---|
| Testcontainers `pgvector/pgvector:pg16` | Ticket rows and `vector(768)` | Use this for integration and API tests |
| H2 | Ticket rows only | A vector test cannot run there |

**Trade-offs.** One Postgres container matches production and makes delete-then-insert real. It is slower than H2. H2 would cover the ticket tables and would skip the index the retrieval tests need. Ollama stays out of the default run so a missing model does not fail the build.

## Unit tests

Unit tests cover code that does not need a database. Collaborators may be fakes. The class under test may not.

| Subject | Assert |
|---|---|
| Section chunker | One description chunk, one chunk per comment, no resolution chunk when the row is absent. A description of 500 words is one chunk. A description of 501 words is two chunks, with `partIndex` `1` and `2`. Comment `groupIndex` starts at 1 in `createdAt`, `id` order. |
| Chunk text | The content prefix matches `spec/rag-ingestion.md`: ticket id, section name, and `Index` only on comments. |
| Metadata | Every chunk has `ticketId`, `status`, `priority`, `assignee`, `category`, and `sectionType`. |
| Transition policy | The 25 decisions in the matrix below. |
| Citation check | An answer id outside the retrieved set becomes the no-match body. The exact reply `No relevant tickets found.` clears `sources` and sets `noRelevantTickets` true. |
| System prompt | The text sent to the model equals the prompt in `spec/rag-api-contract.md`. |

## Integration tests

Integration tests use the real repositories and a real `vector_store`. The embedding client is a fake that returns a 768-dimension vector. It is not the class under test.

| Subject | Assert |
|---|---|
| Ticket and comment rows | Insert, read, comment order, and at most one resolution row. |
| Create id | On an empty database the first id is `TKT-1001`. |
| Delete-then-insert | After a second save, a search does not return the replaced text. Chunks for another ticket remain. |
| Failed embed | The ticket transaction rolls back. Previous chunks remain. |
| Threshold and top-K | Rows are inserted with known vectors. A score below `0.6` is omitted. At most `app.rag.top-k` chunks are kept. A higher score is the closer vector. |

Known vectors are enough for the cutoff. Two identical vectors score closer than two orthogonal vectors. The test fails if the higher number is the worse match, which is the score-direction check in `spec/architecture.md`.

## API tests

API tests call the HTTP API with the real ticket service and the real assistant service. Ollama is replaced by the same fake embedding client and a fake chat model. The fake chat model returns a scripted answer.

| Endpoint | Tests |
|---|---|
| `POST /api/tickets` | `201` with `OPEN`, a `TKT-` id, and empty assignee. Blank description, missing priority, or missing category returns `400`, a `fieldErrors` entry, and no row. Priority other than `HIGH`, `MEDIUM`, or `LOW` returns `400` and `must be HIGH, MEDIUM, or LOW`. `high` is stored as `HIGH`. |
| `GET /api/tickets` | `page=0` and `size=1` returns one row and `total` equal to the stored count. `keyword` and `status` combine with AND. An unknown `status` returns `400`. A `page` past the last page returns `200`, `content` `[]`, and the same `total`. |
| `GET /api/tickets/{id}` | `200` with comments and resolution notes. An unknown id returns `404` and `Ticket TKT-1001 not found`. A bad id pattern returns `400`. |
| `PATCH /api/tickets/{id}` | An empty body returns `400` and `must change at least one field`. An unknown id returns `404`. |
| `POST /api/tickets/{id}/comments` | `201` and the comment is on the next GET. Blank text returns `400`. An unknown ticket returns `404`. |
| `POST /api/ai/ask` | Blank `question` returns `400`. No chunk at or above the threshold returns `200`, `sources` `[]`, `noRelevantTickets` true, and `answer` exactly `No relevant tickets found.`. The chat fake is not called. |
| Errors | `400`, `404`, and `409` bodies have exactly `timestamp`, `status`, `error`, `message`, and `fieldErrors`. `fieldErrors` is empty for `404` and `409`. |
| `503` | A failing embedder on a ticket write returns `Ticket could not be indexed` and stores nothing new. A failing chat call after retrieval returns `Question could not be answered` and writes nothing. |

Every API test that expects a write failure asserts the ticket count, status, assignee, comments, and resolution notes are unchanged.

## State machine test matrix

Each pair is one unit test of the transition policy. Allowed means the policy accepts the pair. Forbidden means it rejects the pair. The API suite then runs one allowed pair and one forbidden pair through `PATCH` so the HTTP result and the index are checked once each, not 25 times.

An allowed API example is `OPEN` to `IN_PROGRESS`: `200`, stored status `IN_PROGRESS`, and that ticket's chunks replaced. A forbidden API example is `OPEN` to `OPEN`: `409`, message `Status cannot change from OPEN to OPEN`, empty `fieldErrors`, same status, and the same chunk ids.

| From | To | Policy test |
|---|---|---|
| `OPEN` | `IN_PROGRESS` | `should_allow_when_openMovesToInProgress` |
| `OPEN` | `RESOLVED` | `should_allow_when_openMovesToResolved` |
| `OPEN` | `CANCELLED` | `should_allow_when_openMovesToCancelled` |
| `IN_PROGRESS` | `OPEN` | `should_allow_when_inProgressMovesToOpen` |
| `IN_PROGRESS` | `RESOLVED` | `should_allow_when_inProgressMovesToResolved` |
| `IN_PROGRESS` | `CANCELLED` | `should_allow_when_inProgressMovesToCancelled` |
| `RESOLVED` | `CLOSED` | `should_allow_when_resolvedMovesToClosed` |
| `RESOLVED` | `OPEN` | `should_allow_when_resolvedMovesToOpen` |
| `CLOSED` | `OPEN` | `should_allow_when_closedMovesToOpen` |
| `CANCELLED` | `OPEN` | `should_allow_when_cancelledMovesToOpen` |
| `OPEN` | `OPEN` | `should_reject_when_openMovesToOpen` |
| `OPEN` | `CLOSED` | `should_reject_when_openMovesToClosed` |
| `IN_PROGRESS` | `IN_PROGRESS` | `should_reject_when_inProgressMovesToInProgress` |
| `IN_PROGRESS` | `CLOSED` | `should_reject_when_inProgressMovesToClosed` |
| `RESOLVED` | `IN_PROGRESS` | `should_reject_when_resolvedMovesToInProgress` |
| `RESOLVED` | `RESOLVED` | `should_reject_when_resolvedMovesToResolved` |
| `RESOLVED` | `CANCELLED` | `should_reject_when_resolvedMovesToCancelled` |
| `CLOSED` | `IN_PROGRESS` | `should_reject_when_closedMovesToInProgress` |
| `CLOSED` | `RESOLVED` | `should_reject_when_closedMovesToResolved` |
| `CLOSED` | `CLOSED` | `should_reject_when_closedMovesToClosed` |
| `CLOSED` | `CANCELLED` | `should_reject_when_closedMovesToCancelled` |
| `CANCELLED` | `IN_PROGRESS` | `should_reject_when_cancelledMovesToInProgress` |
| `CANCELLED` | `RESOLVED` | `should_reject_when_cancelledMovesToResolved` |
| `CANCELLED` | `CLOSED` | `should_reject_when_cancelledMovesToClosed` |
| `CANCELLED` | `CANCELLED` | `should_reject_when_cancelledMovesToCancelled` |

Close is the allowed pair `RESOLVED` to `CLOSED`. The policy test accepts it. The integration test for that pair keeps the resolution chunk and sets metadata `status` to `CLOSED`.

| Where the 25 pairs run | What a failure means | Result |
|---|---|---|
| One unit test per pair, plus two API tests | A bad pair fails fast. HTTP and re-ingestion are checked on one accept and one reject | Use this |
| Twenty-five API tests | Every pair pays for Spring and Postgres | The extra pairs would repeat one mapping |

**Trade-offs.** The policy tests are the matrix the testing rule asks for. They do not boot Spring. Two API tests tie that decision to `200`, `409`, and re-ingestion. A controller that ignored the policy for a pair other than those two would slip through. The mapping is one branch, so the sample is the whole branch.

## Retrieval quality tests

Default tests fix the scores. They do not measure `nomic-embed-text`.

| Check | Setup | Pass |
|---|---|---|
| Hit | The expected ticket's score is the highest and at least `0.6` | That id is in `sources` |
| Miss under the cutoff | The only expected ticket scores `0.59` | `sources` is empty and the answer is the exact no-match message |
| Precision | One expected ticket scores `0.9` and one neighbor scores `0.7` | Both can be returned. Precision is the size of the intersection divided by `sources` size |
| Top-K | Six chunks score above `0.6` and top-K is `5` | Five chunks are passed on, then collapsed to one source per ticket |
| Q09 partial | Only `TKT-1001` is above the cutoff | Hit rate counts a hit. Complete hit fails because `TKT-1003` and `TKT-1006` are missing |

The optional live run loads the eight fixtures and asks Q01 through Q17 from `spec/evaluation-strategy.md`. It prints hit rate at K, precision, and the complete-hit count. It does not fail on those numbers. No pass bar is set, because that corpus has not been scored.

| Run | Model | Failure rule |
|---|---|---|
| Default | Fake vectors with chosen scores | Wrong include or exclude fails the build |
| Optional live run | Ollama `nomic-embed-text` | Metrics are reported. They do not fail the build |

**Trade-offs.** Fixed scores test the gate the API actually uses. They will not catch a weak embedding. The live run is the quality check from the evaluation spec, and a moving score would make the default build flaky. Tune top-K and the threshold with that run, then change the defaults in configuration. Do not bake a one-time score into a test.

## Grounding tests

Default grounding tests script the chat model. They check the service rules, not a live model's wording.

| Scripted model reply | Pass |
|---|---|
| Not called, because nothing passed the threshold | `answer` is exactly `No relevant tickets found.`, `sources` is `[]`, `noRelevantTickets` is true |
| Cites only a retrieved id | `200`, `noRelevantTickets` false, and that id is in `sources` |
| Cites `TKT-9999` when it was not retrieved | The no-match body. `TKT-9999` is not in the response |
| The exact no-match sentence even though chunks were retrieved | `sources` `[]` and `noRelevantTickets` true |
| Q14, any reply | Ticket count, assignees, comments, and resolution notes are unchanged |

The optional live run judges Q01 through Q17 with the checklist in `spec/evaluation-strategy.md`. A person, or a test that looks for the required phrases, marks each answer `grounded`, `partially grounded`, or `hallucinated`. The live run fails when any verdict is not `grounded`. That includes Q02 containing the 8 hour timeout, Q03 containing CSV and PDF, and Q16 not saying 30 minutes. A second model is not the judge.

| Judge | Default build | Result |
|---|---|---|
| Service rules with a scripted reply | Runs | Use this |
| Checklist on live answers | Optional | Fails that run when a phrase or an id is wrong |
| Another model grades the answer | Does not run | A fluent wrong answer can pass |

**Trade-offs.** Scripted replies lock the rules that FR-12 and FR-13 name. They do not show that the real model will mention the 8 hour timeout. The live checklist does that and can fail when the model paraphrases. Keeping it out of the default run avoids a red build from wording. The live run still uses the checklist rather than a model grade.

## Deliberately not tested

These are out of the suites above. A passing build does not claim them.

| Not tested | Why |
|---|---|
| Browser screens in `spec/ui-flow.md` | There is no UI test runner in this strategy. Field messages are covered by the API `fieldErrors` strings. |
| Live hit rate, precision, and complete hits as build gates | No scored baseline exists. The optional run prints them. |
| A numeric similarity target such as "score at least 0.8" | That number would be invented. |
| Cloud embeddings, leaderboard quality, and latency | Not the chosen model, and no measurement is claimed. |
| Auth, roles, and ticket delete | The API has none. |
| Changing description, priority, or category after create | `PATCH` does not accept them. |
| All 25 pairs through HTTP | Two API pairs cover the status and the index. The other 23 are policy tests. |
| Load, concurrency, and HNSW recall versus a full scan | No target is specified. |
| A model judging another model | The grounding spec rejects that judge. |
| Secrets and a real database password | Tests use the container's test password, not a production secret. |
