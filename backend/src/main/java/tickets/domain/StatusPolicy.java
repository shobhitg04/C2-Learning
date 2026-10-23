package tickets.domain;

import java.util.Set;

public final class StatusPolicy {
  private static final Set<String> ALLOWED = Set.of(
      "OPEN|IN_PROGRESS",
      "OPEN|RESOLVED",
      "OPEN|CANCELLED",
      "IN_PROGRESS|OPEN",
      "IN_PROGRESS|RESOLVED",
      "IN_PROGRESS|CANCELLED",
      "RESOLVED|CLOSED",
      "RESOLVED|OPEN",
      "CLOSED|OPEN",
      "CANCELLED|OPEN");

  private StatusPolicy() {}

  public static boolean allowed(Status from, Status to) {
    return ALLOWED.contains(from.name() + "|" + to.name());
  }
}
