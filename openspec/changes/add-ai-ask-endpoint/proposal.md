# Proposal

## Why

`POST /api/ai/ask` is specified and not implemented. Questions are still unanswered, and nothing checks that a reply cites only tickets that retrieval returned.

## What Changes

- Add `POST /api/ai/ask`. `AskController` accepts the question. `AskService` embeds it, searches chunks, and calls the chat model only when at least one chunk is at or above the configured threshold.
- Bind `app.rag.top-k` and `app.rag.similarity-threshold` with `@ConfigurationProperties` and inject that object. The service does not hardcode `5` or `0.6`.
- Group the chunks that pass by `ticketId`. Return one source per ticket: `ticketId`, `title` (the ticket description), and `similarity` (that ticket's highest chunk score). A higher score is closer. A cosine distance is converted with `similarity = 1 - distance` before the threshold check.
- When nothing passes, return `200` immediately with `noRelevantTickets` `true`, `sources` `[]`, and `answer` exactly `No relevant tickets found.` Do not call the chat model.
- Otherwise send the static system prompt and the passing chunks to the chat model named by `spring.ai.ollama.chat.options.model`. The prompt text is the block in `spec/rag-api-contract.md`, which includes every rule in `.cursor/rules/grounding-prompt.md`.
- After generation, if the answer mentions a `TKT-` id that was not retrieved, discard the model text, log a warning with that id, and return the no-match body. The same body is returned when the model replies exactly `No relevant tickets found.`
- A blank question returns `400`. An embedding or chat failure returns `503` `Question could not be answered`. The endpoint writes nothing.

Assumption: the request's `score` is the number already named `similarity` in `spec/rag-api-contract.md` and FR-11. The JSON field stays `similarity`.

## Capabilities

### New Capabilities

- `ai-ask`: Answer one question from retrieved ticket chunks, or return the exact no-match sentence when nothing qualifies.

### Modified Capabilities

## Impact

- New classes `AskController` and `AskService`. Retrieval reads `vector_store` through the existing embedder. There is no similarity search in the app today.
- `pom.xml` gains the Spring AI Ollama chat starter so the chat model comes from configuration. The vector-store starter stays with `add-rag-ingestion`.
- `application.yml` must expose `spring.ai.ollama.chat.options.model`. The root `ai:` block is not bound by Spring Boot.
- `spec/architecture.md` still names `AssistantController` and `AssistantService`. This change uses the requested class names.
- The assistant panel in `spec/ui-flow.md` is unchanged.
