package tickets.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tickets.ai.TicketIndexer;
import tickets.domain.Status;
import tickets.domain.StatusPolicy;
import tickets.domain.Ticket;
import tickets.repository.TicketRepository;

@Service
public class TicketService {
  private final TicketRepository tickets;
  private final TicketIndexer indexer;

  public TicketService(TicketRepository tickets, TicketIndexer indexer) {
    this.tickets = tickets;
    this.indexer = indexer;
  }

  @Transactional
  public Ticket create(String description, String priority, String category, String assignee) {
    var errors = new ArrayList<FieldError>();
    description = required(description, "description", errors);
    priority = priority(priority, errors);
    category = required(category, "category", errors);
    assignee = optional(assignee, "assignee", errors);
    if (!errors.isEmpty()) {
      throw new ApiException(400, "Bad Request", "Validation failed", errors);
    }
    String id = tickets.nextId();
    tickets.insert(id, description, priority, assignee, category);
    reindex(id);
    return tickets.load(id);
  }

  @Transactional(readOnly = true)
  public Ticket get(String id) {
    requireId(id);
    return tickets.find(id).orElseThrow(() -> missing(id));
  }

  @Transactional(readOnly = true)
  public Page list(String keyword, String status, int page, int size) {
    if (status != null && !status.isBlank()) {
      parseStatus(status);
    }
    String filter = status == null ? "" : status;
    long total = tickets.count(keyword, filter);
    var content = tickets.list(keyword, filter, size, page * size);
    return new Page(content, page, size, total);
  }

  @Transactional
  public Ticket addComment(String id, String body) {
    requireId(id);
    if (tickets.find(id).isEmpty()) {
      throw missing(id);
    }
    if (body == null || body.isBlank()) {
      throw new ApiException(400, "Bad Request", "Validation failed", List.of(new FieldError("body", "must not be blank")));
    }
    tickets.addComment(id, body.trim());
    reindex(id);
    return tickets.load(id);
  }

  @Transactional
  public Ticket patch(String id, boolean statusSet, String status, boolean assigneeSet, String assignee, boolean notesSet, String notes, boolean anyField) {
    requireId(id);
    Ticket current = tickets.find(id).orElseThrow(() -> missing(id));
    if (!anyField) {
      throw new ApiException(400, "Bad Request", "Validation failed", List.of(new FieldError("body", "must change at least one field")));
    }
    Status next = current.status();
    if (statusSet) {
      next = parseStatus(status);
      if (!StatusPolicy.allowed(current.status(), next)) {
        throw new ApiException(409, "Conflict", "Status cannot change from " + current.status() + " to " + next, List.of());
      }
    }
    String nextAssignee = current.assignee();
    if (assigneeSet) {
      nextAssignee = optional(assignee, "assignee", new ArrayList<>());
      if (assignee != null && assignee.isBlank()) {
        throw new ApiException(400, "Bad Request", "Validation failed", List.of(new FieldError("assignee", "must not be blank")));
      }
      if (assignee != null) {
        nextAssignee = assignee.trim();
      }
    }
    if (notesSet && notes != null && notes.isBlank()) {
      throw new ApiException(400, "Bad Request", "Validation failed", List.of(new FieldError("resolutionNotes", "must not be blank")));
    }
    tickets.update(id, next, nextAssignee);
    if (notesSet) {
      tickets.saveNotes(id, notes == null ? null : notes.trim());
    }
    reindex(id);
    return tickets.load(id);
  }

  private void reindex(String id) {
    try {
      indexer.reindex(id);
    } catch (RuntimeException ex) {
      throw new ApiException(503, "Service Unavailable", "Ticket could not be indexed", List.of());
    }
  }

  private static void requireId(String id) {
    if (id == null || !id.matches("TKT-[0-9]+")) {
      throw new ApiException(400, "Bad Request", "Validation failed", List.of(new FieldError("id", "must match TKT- and digits")));
    }
  }

  private static Status parseStatus(String raw) {
    try {
      return Status.valueOf(raw.trim());
    } catch (RuntimeException ex) {
      throw new ApiException(400, "Bad Request", "Validation failed", List.of(new FieldError("status", "must be OPEN, IN_PROGRESS, RESOLVED, CLOSED, or CANCELLED")));
    }
  }

  private static String required(String value, String field, List<FieldError> errors) {
    if (value == null || value.isBlank()) {
      errors.add(new FieldError(field, "must not be blank"));
      return "";
    }
    return value.trim();
  }

  private static String optional(String value, String field, List<FieldError> errors) {
    if (value == null) {
      return null;
    }
    if (value.isBlank()) {
      errors.add(new FieldError(field, "must not be blank"));
      return null;
    }
    return value.trim();
  }

  private static String priority(String value, List<FieldError> errors) {
    if (value == null || value.isBlank()) {
      errors.add(new FieldError("priority", "must not be blank"));
      return "";
    }
    String upper = value.trim().toUpperCase();
    if (!upper.equals("HIGH") && !upper.equals("MEDIUM") && !upper.equals("LOW")) {
      errors.add(new FieldError("priority", "must be HIGH, MEDIUM, or LOW"));
      return "";
    }
    return upper;
  }

  private static ApiException missing(String id) {
    return new ApiException(404, "Not Found", "Ticket " + id + " not found", List.of());
  }

  public record Page(List<Ticket> content, int page, int size, long total) {}

  public record FieldError(String field, String message) {}

  public static final class ApiException extends RuntimeException {
    private final int status;
    private final String error;
    private final List<FieldError> fields;

    public ApiException(int status, String error, String message, List<FieldError> fields) {
      super(message);
      this.status = status;
      this.error = error;
      this.fields = fields;
    }

    public int status() {
      return status;
    }

    public String error() {
      return error;
    }

    public List<FieldError> fields() {
      return fields;
    }
  }
}
