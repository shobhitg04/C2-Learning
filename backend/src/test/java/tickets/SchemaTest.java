package tickets;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class SchemaTest extends PgTest {
  @Autowired JdbcTemplate jdbc;

  @Test
  void should_start_when_databaseIsPgvector() {
    assertThat(jdbc.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
  }

  @Test
  void should_createObjects_when_schemaRuns() {
    assertThat(jdbc.queryForObject("SELECT start_value FROM pg_sequences WHERE sequencename = 'ticket_id_seq'", Long.class)).isEqualTo(1001L);
    assertThat(jdbc.queryForObject("SELECT to_regclass('ticket')", String.class)).isEqualTo("ticket");
    assertThat(jdbc.queryForObject("SELECT to_regclass('comment')", String.class)).isEqualTo("comment");
    assertThat(jdbc.queryForObject("SELECT to_regclass('ticket_resolution')", String.class)).isEqualTo("ticket_resolution");
    assertThat(jdbc.queryForObject("SELECT to_regclass('vector_store')", String.class)).isEqualTo("vector_store");
  }
}
