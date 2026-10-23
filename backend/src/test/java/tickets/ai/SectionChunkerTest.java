package tickets.ai;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import tickets.domain.Status;
import tickets.domain.Ticket;

class SectionChunkerTest {
  @Test
  void should_keepOneChunk_when_descriptionHas500Words() {
    var docs = SectionChunker.chunk(ticket(String.join(" ", Collections.nCopies(500, "word"))));
    assertThat(docs).hasSize(1);
    assertThat(docs.get(0).partIndex()).isNull();
  }

  @Test
  void should_split_when_descriptionHas501Words() {
    var docs = SectionChunker.chunk(ticket(String.join(" ", Collections.nCopies(501, "word"))));
    assertThat(docs).hasSize(2);
    assertThat(docs.get(0).partIndex()).isEqualTo(1);
    assertThat(docs.get(1).partIndex()).isEqualTo(2);
  }

  private static Ticket ticket(String description) {
    return new Ticket("TKT-1001", description, Status.OPEN, "HIGH", null, "billing", List.of(), null);
  }
}
