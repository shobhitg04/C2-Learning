# Design

## Context

`backend/src/main/resources/application.yml` sets the datasource and `app.ollama.url`. It does not set `server.port`, so Spring Boot uses 8080. See proposal.md for why the port changes. API tests use MockMvc and do not open a server socket.

## Goals / Non-Goals

**Goals:**

- Make port 85 the configured listen port for the backend process.

**Non-Goals:**

- Changing the Postgres or Ollama ports.
- Publishing the application from `docker-compose.yml`.
- Adding an environment variable that falls back to 8080.

## Decisions

### Fixed `server.port`

Add `server.port: 85` to `backend/src/main/resources/application.yml`. Tests keep the mock web environment, so they read the property and do not bind port 85.

Alternative: `server.port: ${SERVER_PORT:85}`. Rejected because an exported `SERVER_PORT` would silently leave 85.

Alternative: keep 8080 and map 85 in Compose. Rejected because Compose does not run the application, and the request is the backend listen port.

## Risks / Trade-offs

- [Port 85 is privileged on macOS and Linux] → A normal user process fails at startup with an address bind error. Run the backend as root, or grant the process permission to bind port 85. Tests do not bind it.
- [Clients and the browser still assume 8080] → Nothing in `spec/` names 8080 today. `spec/architecture.md` gains the listen port so the new value is written down.

## Migration Plan

1. Set `server.port` to 85.
2. Restart the backend. Roll back by removing the property, which returns the process to 8080.

## Open Questions

None.
