# Proposal

## Why

The backend does not set `server.port`, so Spring Boot listens on 8080. The ticket API should listen on port 85.

## What Changes

- Set the backend HTTP listen port to `85`.
- Leave Postgres on `5432` and Ollama on `11434`.

Assumption: `85` is a fixed `server.port` value, not an environment override that falls back to `8080`.

## Capabilities

### New Capabilities

- `backend-server`: The HTTP server listen port.

### Modified Capabilities

## Impact

- `backend/src/main/resources/application.yml` has datasource and Ollama settings and no `server.port`.
- `docker-compose.yml` publishes only Postgres. It does not publish the application port.
- Existing API tests use MockMvc and do not bind a listen port. `spec/architecture.md` does not name an HTTP port.
- Port `85` is below 1024, so a non-root process on macOS or Linux cannot bind it.
