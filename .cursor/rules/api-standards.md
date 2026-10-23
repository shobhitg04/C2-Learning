---
description: REST naming, status codes, errors, and pagination
alwaysApply: true
---

# API Standards

- Name collections as plural nouns, for example `/api/tickets`.
- Nest sub-resources under the parent, for example `/api/tickets/{id}/comments`.
- Return `201` when a resource is created.
- Return `400` when request validation fails.
- Return `404` when the resource does not exist.
- Return `409` when a state transition is invalid.
- Return every error as one body: `{timestamp, status, error, message, fieldErrors[]}`.
- Paginate every list endpoint.
- Handle every error in a single `@RestControllerAdvice`.
