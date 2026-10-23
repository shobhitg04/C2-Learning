package tickets.domain;

import java.time.Instant;

public record Comment(long id, String body, Instant createdAt) {}
