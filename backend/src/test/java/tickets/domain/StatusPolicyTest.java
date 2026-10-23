package tickets.domain;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class StatusPolicyTest {
  @Test void should_allow_when_openMovesToInProgress() { assertThat(StatusPolicy.allowed(Status.OPEN, Status.IN_PROGRESS)).isTrue(); }
  @Test void should_allow_when_openMovesToResolved() { assertThat(StatusPolicy.allowed(Status.OPEN, Status.RESOLVED)).isTrue(); }
  @Test void should_allow_when_openMovesToCancelled() { assertThat(StatusPolicy.allowed(Status.OPEN, Status.CANCELLED)).isTrue(); }
  @Test void should_allow_when_inProgressMovesToOpen() { assertThat(StatusPolicy.allowed(Status.IN_PROGRESS, Status.OPEN)).isTrue(); }
  @Test void should_allow_when_inProgressMovesToResolved() { assertThat(StatusPolicy.allowed(Status.IN_PROGRESS, Status.RESOLVED)).isTrue(); }
  @Test void should_allow_when_inProgressMovesToCancelled() { assertThat(StatusPolicy.allowed(Status.IN_PROGRESS, Status.CANCELLED)).isTrue(); }
  @Test void should_allow_when_resolvedMovesToClosed() { assertThat(StatusPolicy.allowed(Status.RESOLVED, Status.CLOSED)).isTrue(); }
  @Test void should_allow_when_resolvedMovesToOpen() { assertThat(StatusPolicy.allowed(Status.RESOLVED, Status.OPEN)).isTrue(); }
  @Test void should_reject_when_openMovesToOpen() { assertThat(StatusPolicy.allowed(Status.OPEN, Status.OPEN)).isFalse(); }
  @Test void should_reject_when_openMovesToClosed() { assertThat(StatusPolicy.allowed(Status.OPEN, Status.CLOSED)).isFalse(); }
  @Test void should_reject_when_inProgressMovesToInProgress() { assertThat(StatusPolicy.allowed(Status.IN_PROGRESS, Status.IN_PROGRESS)).isFalse(); }
  @Test void should_reject_when_inProgressMovesToClosed() { assertThat(StatusPolicy.allowed(Status.IN_PROGRESS, Status.CLOSED)).isFalse(); }
  @Test void should_reject_when_resolvedMovesToInProgress() { assertThat(StatusPolicy.allowed(Status.RESOLVED, Status.IN_PROGRESS)).isFalse(); }
  @Test void should_reject_when_resolvedMovesToResolved() { assertThat(StatusPolicy.allowed(Status.RESOLVED, Status.RESOLVED)).isFalse(); }
  @Test void should_reject_when_resolvedMovesToCancelled() { assertThat(StatusPolicy.allowed(Status.RESOLVED, Status.CANCELLED)).isFalse(); }
  @Test void should_reject_when_closedMovesToInProgress() { assertThat(StatusPolicy.allowed(Status.CLOSED, Status.IN_PROGRESS)).isFalse(); }
  @Test void should_reject_when_closedMovesToResolved() { assertThat(StatusPolicy.allowed(Status.CLOSED, Status.RESOLVED)).isFalse(); }
  @Test void should_reject_when_closedMovesToClosed() { assertThat(StatusPolicy.allowed(Status.CLOSED, Status.CLOSED)).isFalse(); }
  @Test void should_reject_when_closedMovesToCancelled() { assertThat(StatusPolicy.allowed(Status.CLOSED, Status.CANCELLED)).isFalse(); }
  @Test void should_allow_when_closedMovesToOpen() { assertThat(StatusPolicy.allowed(Status.CLOSED, Status.OPEN)).isTrue(); }
  @Test void should_allow_when_cancelledMovesToOpen() { assertThat(StatusPolicy.allowed(Status.CANCELLED, Status.OPEN)).isTrue(); }
  @Test void should_reject_when_cancelledMovesToInProgress() { assertThat(StatusPolicy.allowed(Status.CANCELLED, Status.IN_PROGRESS)).isFalse(); }
  @Test void should_reject_when_cancelledMovesToResolved() { assertThat(StatusPolicy.allowed(Status.CANCELLED, Status.RESOLVED)).isFalse(); }
  @Test void should_reject_when_cancelledMovesToClosed() { assertThat(StatusPolicy.allowed(Status.CANCELLED, Status.CLOSED)).isFalse(); }
  @Test void should_reject_when_cancelledMovesToCancelled() { assertThat(StatusPolicy.allowed(Status.CANCELLED, Status.CANCELLED)).isFalse(); }
}
