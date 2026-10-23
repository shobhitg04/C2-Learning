# Generate tests

Given a class or spec section, generate JUnit 5 tests following `.cursor/rules/testing.md`.

## Steps

1. Read `.cursor/rules/testing.md` and follow it.
2. Read the class or spec section to test.
3. Cover the happy path.
4. Cover validation failures.
5. Cover every forbidden state transition.
6. List any behaviour you could not test and why.

## Output format

```java
// JUnit 5 tests named should_expectedBehaviour_when_condition
```

### Could not test

- Behaviour — why it could not be tested.
