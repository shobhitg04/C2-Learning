package tickets;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import tickets.repository.TicketRepository;

@SpringBootTest
class TicketRepositoryTest extends PgTest {
  @Autowired TicketRepository tickets;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void reset() {
    jdbc.update("DELETE FROM vector_store");
    jdbc.update("TRUNCATE ticket CASCADE");
    jdbc.execute("SELECT setval('ticket_id_seq', 1001, false)");
  }

  @Test
  void should_assignTkt1001_when_databaseIsEmpty() {
    String id = tickets.nextId();
    tickets.insert(id, "Invoice export fails", "HIGH", null, "billing");
    assertThat(id).isEqualTo("TKT-1001");
    assertThat(tickets.find(id)).isPresent();
  }

  @Test
  void should_orderCommentsAndKeepOneResolution_when_rowsAreSaved() {
    tickets.insert("TKT-1001", "Invoice export fails", "HIGH", null, "billing");
    long first = tickets.addComment("TKT-1001", "first");
    long second = tickets.addComment("TKT-1001", "second");
    jdbc.update("UPDATE comment SET created_at = now() - interval '1 day' WHERE id = ?", second);
    assertThat(tickets.find("TKT-1001").orElseThrow().comments()).extracting(c -> c.id()).containsExactly(second, first);
    tickets.saveNotes("TKT-1001", "one");
    tickets.saveNotes("TKT-1001", "two");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_resolution", Long.class)).isEqualTo(1L);
    assertThat(tickets.find("TKT-1001").orElseThrow().resolutionNotes()).isEqualTo("two");
  }
}
