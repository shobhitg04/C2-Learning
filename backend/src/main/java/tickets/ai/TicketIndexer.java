package tickets.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tickets.domain.Ticket;
import tickets.repository.TicketRepository;

@Component
public class TicketIndexer {
  private final TicketRepository tickets;
  private final JdbcTemplate jdbc;
  private final Embedder embedder;
  private final ObjectMapper json;

  public TicketIndexer(TicketRepository tickets, JdbcTemplate jdbc, Embedder embedder, ObjectMapper json) {
    this.tickets = tickets;
    this.jdbc = jdbc;
    this.embedder = embedder;
    this.json = json;
  }

  public void reindex(String ticketId) {
    Ticket ticket = tickets.find(ticketId).orElseThrow();
    jdbc.update("DELETE FROM vector_store WHERE metadata->>'ticketId' = ?", ticketId);
    for (var doc : SectionChunker.chunk(ticket)) {
      var meta = new LinkedHashMap<String, Object>();
      meta.put("ticketId", ticket.id());
      meta.put("status", ticket.status().name());
      meta.put("priority", ticket.priority());
      meta.put("assignee", ticket.assignee());
      meta.put("category", ticket.category());
      meta.put("sectionType", doc.sectionType());
      if (doc.groupIndex() != null) {
        meta.put("groupIndex", doc.groupIndex());
      }
      if (doc.partIndex() != null) {
        meta.put("partIndex", doc.partIndex());
      }
      try {
        jdbc.update(
            "INSERT INTO vector_store (content, metadata, embedding) VALUES (?, ?::json, ?::vector)",
            doc.content(),
            json.writeValueAsString(meta),
            vector(embedder.embed(doc.content())));
      } catch (RuntimeException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new IllegalStateException("embedding failed", ex);
      }
    }
  }

  public static String vector(float[] values) {
    var text = new StringBuilder("[");
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        text.append(',');
      }
      text.append(values[i]);
    }
    return text.append(']').toString();
  }
}
