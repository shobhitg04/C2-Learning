# Tasks

## 1. Ask endpoint

- [x] 1.1 Add `RagProperties` bound to `app.rag` and inject it into `AskService`. Search the nearest `topK` chunks in `vector_store`, convert cosine distance with `similarity = 1 - distance`, and keep a chunk only when its similarity is at or above `similarityThreshold`. Group by `ticketId`, set `title` from the ticket description, and set `similarity` to the highest score. Verify a score below the threshold is omitted, two chunks of one ticket become one source, and overriding either property changes the value used.
- [x] 1.2 Add `POST /api/ai/ask`. A blank `question` returns `400`, field `question`, message `must not be blank`. When nothing passes, return `200` with `noRelevantTickets` `true`, `sources` `[]`, and `answer` exactly `No relevant tickets found.`, and do not call the chat model. Otherwise call the chat model named by `spring.ai.ollama.chat.options.model` with the static prompt in `spec/rag-api-contract.md` and the passing chunks. Add the Spring AI 1.0 BOM if it is missing, add `spring-ai-starter-model-ollama`, and set that chat property from the existing model value. Verify a grounded reply returns `200`, the retrieved sources, and `noRelevantTickets` `false`, and that the stored ticket count is unchanged.
- [x] 1.3 If the answer contains a `TKT-` id outside the retrieved set, log a warning with that id and return the no-match body. Do the same, without a warning, when the model replies exactly `No relevant tickets found.` A failed embed or a failed chat call returns `503` with message `Question could not be answered` and writes nothing. Add the warning sentence to the Grounding section of `spec/rag-api-contract.md`, and rename `AssistantController` and `AssistantService` to `AskController` and `AskService` in `spec/architecture.md`. Verify an invented id becomes the no-match body, and a chat failure returns `503` without changing tickets.

## Workflow follow-up

- If `add-rag-ingestion` later replaces JDBC search with `VectorStore`, keep the threshold, grouping, and citation check in `AskService`.
- Archive with `/opsx-archive` after review.
