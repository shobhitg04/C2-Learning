---
description: JUnit 5 and AssertJ conventions for Java tests
globs: "**/*Test*.java"
alwaysApply: false
---

# Testing

- Write tests with JUnit 5 and AssertJ.
- Use `@SpringBootTest` only for integration tests.
- Test repositories with Testcontainers or H2.
- Write one test for each allowed state transition.
- Write one test for each forbidden state transition.
- Name every test `should_expectedBehaviour_when_condition`.
- Never mock the class under test.
