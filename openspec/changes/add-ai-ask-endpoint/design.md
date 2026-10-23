# Design

## Context

`POST /api/ai/ask` is specified in `spec/rag-api-contract.md` and is not implemented. `TicketIndexer` writes `vector_store` with JDBC and `Embedder`. Nothing searches that table. `application.yml` already sets `app.rag.top-k` and `app.rag.similarity-threshold`. The Ollama chat settings sit under a root `ai:` key, so Boot does not bind `spring.ai.ollama.chat.options.model`. `pom.xml` has no Spring AI dependency. See proposal.md for why the JSON field stays `similarity`.

## Goals / Non-Goals

**Goals:**

- Answer `POST /api/ai/ask` from retrieved chunks, and skip the chat model when none pass the threshold.
- Discard an answer that cites a ticket id outside the retrieved set, and log a warning.
- Read top-K and the threshold from configuration.

**Non-Goals:**

- The assistant panel.
- Replacing `TicketIndexer` or adding the PgVector starter.
- Renaming `similarity` to `score`.

## Decisions

### AskService searches, then maybe chats

`AskController` lives in `tickets.controller`. `AskService` lives in `tickets.ai`. The controller maps `POST /api/ai/ask` and returns the response record. The service trims `question`, rejects blank with the existing `400` error type, embeds the question with `Embedder`, and reads the nearest `topK` rows from `vector_store`.

The query orders by cosine distance `<=>` and limits to the configured top-K. `similarity = 1 - distance`. A chunk is kept when `similarity` is greater than or equal to the configured threshold. Chunks are grouped by `metadata.ticketId`. `title` is that ticket's `description` from `TicketRepository`. The service does not call `TicketService` and does not write.

`RagProperties` is a `@ConfigurationProperties(prefix = "app.rag")` object with `topK` and `similarityThreshold`, constructor-injected into `AskService`. The numbers `5` and `0.6` appear only as defaults in `application.yml`.

Alternative: hardcode the cutoff in the service. Rejected because NFR-6 says an override must change the value the retriever receives.

### Chat only after a hit

An empty passing set returns the no-match body and does not call the chat model.

Otherwise `ChatClient` is called with one static system prompt, the prompt block in the Grounding section of `spec/rag-api-contract.md`, and the passing chunk texts grouped by ticket id. That block already contains the four rules in `.cursor/rules/grounding-prompt.md`. The model name is not set in Java. Spring AI reads `spring.ai.ollama.chat.options.model`.

This change adds the Spring AI 1.0 BOM, if `pom.xml` does not already import it, and `spring-ai-starter-model-ollama`. It copies the chat model and `temperature: 0` under `spring.ai.ollama`. It does not set `spring.ai.vectorstore.pgvector.initialize-schema`.

Alternative: an HTTP client beside `OllamaEmbedder`. Rejected because the architecture names the Spring AI chat model and the property `spring.ai.ollama.chat.options.model`.

### Citation check after generation

A ticket key is the pattern `TKT-` plus digits, with or without brackets. Every match must be in the retrieved id set. Any other id is logged at warning level with that id, the model text is dropped, and the no-match body is returned. The exact model reply `No relevant tickets found.` also returns that body, with no warning. A grounded answer returns the model text, the grouped sources, and `noRelevantTickets` `false`.

An embed failure, or a chat failure after retrieval, becomes `503` with message `Question could not be answered` and an empty `fieldErrors`. The handler is the existing controller advice.

Tests fake `Embedder` and the chat model. The default build does not call Ollama. One test overrides `app.rag.top-k` and `app.rag.similarity-threshold` and shows the service used those values.

## Risks / Trade-offs

- [Discarding an ungrounded answer drops facts that were retrieved] → The contract chooses the no-match body over returning an invented id. The warning records the id that failed the check.
- [JDBC search duplicates a `VectorStore` query `add-rag-ingestion` may add later] → The limits, grouping, and citation check stay in `AskService`. The search call can move to `VectorStore` without changing the HTTP contract.
- [The chat starter can also auto-configure an embedding model] → Tests replace the chat model and the existing `Embedder`. Question embedding keeps using `Embedder`.

## Migration Plan

1. Add the properties object, the search, and the endpoint.
2. Point `spring.ai.ollama.chat.options.model` at the model value already in the root `ai:` block.
3. Roll back by removing the route and the chat starter. Stored tickets are untouched either way.

## Open Questions

None. `title` is the ticket description. One source is returned per retrieved ticket.
