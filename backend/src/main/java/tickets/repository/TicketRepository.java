package tickets.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tickets.domain.Comment;
import tickets.domain.Status;
import tickets.domain.Ticket;

@Repository
public class TicketRepository {
  private final JdbcTemplate jdbc;

  public TicketRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public String nextId() {
    Long n = jdbc.queryForObject("SELECT nextval('ticket_id_seq')", Long.class);
    return "TKT-" + n;
  }

  public void insert(String id, String description, String priority, String assignee, String category) {
    jdbc.update(
        "INSERT INTO ticket (id, description, status, priority, assignee, category) VALUES (?, ?, 'OPEN', ?, ?, ?)",
        id, description, priority, assignee, category);
  }

  public Optional<Ticket> find(String id) {
    Long count = jdbc.queryForObject("SELECT count(*) FROM ticket WHERE id = ?", Long.class, id);
    if (count == null || count == 0) {
      return Optional.empty();
    }
    return Optional.of(load(id));
  }

  public Ticket load(String id) {
    return jdbc.queryForObject(
        "SELECT id, description, status, priority, assignee, category FROM ticket WHERE id = ?",
        (rs, n) -> new Ticket(
            rs.getString("id"),
            rs.getString("description"),
            Status.valueOf(rs.getString("status")),
            rs.getString("priority"),
            rs.getString("assignee"),
            rs.getString("category"),
            comments(id),
            notes(id)),
        id);
  }

  public List<Comment> comments(String ticketId) {
    return jdbc.query(
        "SELECT id, body, created_at FROM comment WHERE ticket_id = ? ORDER BY created_at, id",
        (rs, n) -> new Comment(
            rs.getLong("id"),
            rs.getString("body"),
            rs.getObject("created_at", OffsetDateTime.class).toInstant()),
        ticketId);
  }

  public long addComment(String ticketId, String body) {
    return jdbc.queryForObject(
        "INSERT INTO comment (ticket_id, body) VALUES (?, ?) RETURNING id",
        Long.class,
        ticketId,
        body);
  }

  public void update(String id, Status status, String assignee) {
    jdbc.update("UPDATE ticket SET status = ?, assignee = ? WHERE id = ?", status.name(), assignee, id);
  }

  public void saveNotes(String ticketId, String notes) {
    if (notes == null) {
      jdbc.update("DELETE FROM ticket_resolution WHERE ticket_id = ?", ticketId);
      return;
    }
    jdbc.update(
        """
        INSERT INTO ticket_resolution (ticket_id, notes) VALUES (?, ?)
        ON CONFLICT (ticket_id) DO UPDATE SET notes = EXCLUDED.notes
        """,
        ticketId,
        notes);
  }

  private String notes(String ticketId) {
    var rows = jdbc.query(
        "SELECT notes FROM ticket_resolution WHERE ticket_id = ?",
        (rs, n) -> rs.getString("notes"),
        ticketId);
    return rows.isEmpty() ? null : rows.get(0);
  }

  public List<Ticket> list(String keyword, String status, int limit, int offset) {
    var args = filterArgs(keyword, status);
    var all = new Object[args.length + 2];
    System.arraycopy(args, 0, all, 0, args.length);
    all[args.length] = limit;
    all[args.length + 1] = offset;
    return jdbc.query(listSql(false), (rs, n) -> load(rs.getString("id")), all);
  }

  public long count(String keyword, String status) {
    Long count = jdbc.queryForObject(listSql(true), Long.class, filterArgs(keyword, status));
    return count == null ? 0 : count;
  }

  private static Object[] filterArgs(String keyword, String status) {
    String pattern = like(keyword);
    String statusFlag = status == null || status.isBlank() ? "" : status;
    return new Object[] {
      pattern, pattern, pattern, pattern, pattern, pattern, statusFlag, statusFlag
    };
  }

  private static String listSql(boolean count) {
    var where = """
        WHERE (? = '' OR id ILIKE ? ESCAPE '\\' OR description ILIKE ? ESCAPE '\\' OR category ILIKE ? ESCAPE '\\'
          OR EXISTS (SELECT 1 FROM comment c WHERE c.ticket_id = ticket.id AND c.body ILIKE ? ESCAPE '\\')
          OR EXISTS (SELECT 1 FROM ticket_resolution r WHERE r.ticket_id = ticket.id AND r.notes ILIKE ? ESCAPE '\\'))
        AND (? = '' OR status = ?)
        """;
    if (count) {
      return "SELECT count(*) FROM ticket " + where;
    }
    return "SELECT id FROM ticket " + where + " ORDER BY CAST(substring(id FROM 5) AS INTEGER) LIMIT ? OFFSET ?";
  }

  private static String like(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return "";
    }
    var escaped = keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return "%" + escaped + "%";
  }
}
