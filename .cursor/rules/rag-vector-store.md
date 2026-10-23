---
description: RAG chunking, embeddings, metadata, and retrieval limits
alwaysApply: true
---

# RAG Vector Store

- Chunk a ticket as one chunk per section: the description, each comment group, and the resolution notes.
- Embed chunks with `nomic-embed-text` through Ollama, using 768 dimensions.
- Store this metadata on every chunk: `ticketId`, `status`, `priority`, `assignee`, and `category`.
- Read the retrieval count from `app.rag.top-k`. Default it to `5`.
- Read the minimum score from `app.rag.similarity-threshold`. Default it to `0.6`.
- Re-ingest a ticket by deleting its existing chunks, then inserting the new chunks.
- Never answer from general LLM knowledge. Answer only from retrieved ticket context.
