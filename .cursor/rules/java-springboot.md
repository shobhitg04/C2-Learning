---
description: Java 21 and Spring Boot 3 layered application conventions
alwaysApply: true
---

# Java and Spring Boot

- Target Java 21 and Spring Boot 3.
- Inject dependencies through constructors only.
- Do not use field injection or `@Autowired` on fields.
- Model request and response DTOs as records.
- Organize code into layered packages: `controller`, `service`, `repository`, `domain`, and `ai`.
- Keep business logic out of controllers. A controller validates input, calls a service, and maps the response.
- Put `@Transactional` on service methods that write or require a consistent read.
- Apply Bean Validation annotations on request DTOs, and `@Valid` on controller method arguments.
- Do not put secrets in source code. Read them from environment variables.
