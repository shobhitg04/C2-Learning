# Tasks

## 1. Listen port

- [ ] 1.1 Set `server.port` to `85` in `backend/src/main/resources/application.yml`. Add one sentence to `spec/architecture.md` that the backend HTTP server listens on port 85, with a trade-off sentence for the privileged port. Verify the YAML value is `85` and `spec/architecture.md` states port 85.
- [ ] 1.2 Verify a `@SpringBootTest` reads `server.port` as `85` and does not bind that port. Verify `./mvnw -q test` from `backend/` exits 0.

## Workflow follow-up

- Archive with `/opsx-archive` after review.
