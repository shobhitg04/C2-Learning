# Proposal

## Why

Support work is still tracked outside this repository. The behavior for tickets is already written in `spec/`, and Postgres is defined in `docker-compose.yml`, but there is no application that can create, find, or advance a ticket.

## What Changes

- Add a ticket HTTP API: create, get, list with keyword and status filters, partial update, and comments.
- Enforce the five-status workflow. Allowed moves return `200` and re-index that ticket. Forbidden moves return `409` and leave the ticket and its chunks unchanged.
- Persist tickets, comments, and resolution notes in PostgreSQL, with ids `TKT-` plus digits starting at `TKT-1001` on an empty database.
- Add the list, detail, create, and edit screens from `spec/ui-flow.md`.
- Ask questions, the assistant panel, retrieval quality, and grounding stay out of this change. A successful ticket write still re-indexes that ticket so a later ask change can read current chunks.

## Capabilities

### New Capabilities

- `tickets`: Create, find, comment on, and update support tickets, including the allowed status transitions and the re-index that follows a successful write.

### Modified Capabilities

- None. `openspec/specs/` has no capabilities yet.

## Impact

- New Spring Boot 3 service on Java 21. No application source exists today.
- PostgreSQL 16 with pgvector, database `tickets`, from the existing Compose file.
- Endpoints under `/api/tickets` and `/api/tickets/{id}/comments`.
- `POST /api/ai/ask` and `spring.ai.ollama.chat.options.model` are not part of this change.
- A successful create, comment, assignee change, resolution-note change, or allowed status change calls the ticket indexer in the same transaction. The indexer behavior is specified in `spec/rag-ingestion.md`.
