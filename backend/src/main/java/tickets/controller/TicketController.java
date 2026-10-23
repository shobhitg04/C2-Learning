package tickets.controller;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tickets.domain.Comment;
import tickets.domain.Ticket;
import tickets.service.TicketService;
import tickets.service.TicketService.ApiException;
import tickets.service.TicketService.FieldError;

@RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/tickets")
public class TicketController {
  private final TicketService tickets;

  public TicketController(TicketService tickets) {
    this.tickets = tickets;
  }

  @PostMapping
  public ResponseEntity<TicketDetail> create(@RequestBody JsonNode body) {
    if (body.hasNonNull("status")) {
      throw new ApiException(400, "Bad Request", "Validation failed", List.of(new FieldError("status", "must not be set")));
    }
    Ticket ticket = tickets.create(text(body, "description"), text(body, "priority"), text(body, "category"), text(body, "assignee"));
    return ResponseEntity.status(201).body(TicketDetail.of(ticket));
  }

  @GetMapping("/{id}")
  public TicketDetail get(@PathVariable String id) {
    return TicketDetail.of(tickets.get(id));
  }

  @GetMapping
  public PageBody list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") String page,
      @RequestParam(defaultValue = "20") String size) {
    int pageNo = integer(page, "page", "must be zero or greater");
    int sizeNo = integer(size, "size", "must be between 1 and 100");
    if (pageNo < 0) {
      throw new ApiException(400, "Bad Request", "Validation failed", List.of(new FieldError("page", "must be zero or greater")));
    }
    if (sizeNo < 1 || sizeNo > 100) {
      throw new ApiException(400, "Bad Request", "Validation failed", List.of(new FieldError("size", "must be between 1 and 100")));
    }
    var found = tickets.list(keyword, status, pageNo, sizeNo);
    return new PageBody(found.content().stream().map(TicketSummary::of).toList(), found.page(), found.size(), found.total());
  }

  @PatchMapping("/{id}")
  public TicketDetail patch(@PathVariable String id, @RequestBody JsonNode body) {
    boolean statusSet = body.has("status");
    boolean assigneeSet = body.has("assignee");
    boolean notesSet = body.has("resolutionNotes");
    return TicketDetail.of(tickets.patch(
        id,
        statusSet,
        statusSet && !body.get("status").isNull() ? body.get("status").asText() : null,
        assigneeSet,
        assigneeSet && !body.get("assignee").isNull() ? body.get("assignee").asText() : null,
        notesSet,
        notesSet && !body.get("resolutionNotes").isNull() ? body.get("resolutionNotes").asText() : null,
        statusSet || assigneeSet || notesSet));
  }

  @PostMapping("/{id}/comments")
  public ResponseEntity<CommentBody> comment(@PathVariable String id, @RequestBody JsonNode body) {
    Ticket ticket = tickets.addComment(id, text(body, "body"));
    Comment comment = ticket.comments().get(ticket.comments().size() - 1);
    return ResponseEntity.status(201).body(new CommentBody(comment.id(), comment.body(), comment.createdAt()));
  }

  private static String text(JsonNode body, String field) {
    if (!body.has(field) || body.get(field).isNull()) {
      return null;
    }
    return body.get(field).asText();
  }

  private static int integer(String raw, String field, String message) {
    try {
      return Integer.parseInt(raw);
    } catch (NumberFormatException ex) {
      throw new ApiException(400, "Bad Request", "Validation failed", List.of(new FieldError(field, message)));
    }
  }

  public record TicketSummary(String id, String description, String status, String priority, String assignee, String category) {
    static TicketSummary of(Ticket ticket) {
      return new TicketSummary(ticket.id(), ticket.description(), ticket.status().name(), ticket.priority(), ticket.assignee(), ticket.category());
    }
  }

  public record CommentBody(long id, String body, Instant createdAt) {}

  public record TicketDetail(
      String id,
      String description,
      String status,
      String priority,
      String assignee,
      String category,
      List<CommentBody> comments,
      String resolutionNotes) {
    static TicketDetail of(Ticket ticket) {
      return new TicketDetail(
          ticket.id(),
          ticket.description(),
          ticket.status().name(),
          ticket.priority(),
          ticket.assignee(),
          ticket.category(),
          ticket.comments().stream().map(c -> new CommentBody(c.id(), c.body(), c.createdAt())).toList(),
          ticket.resolutionNotes());
    }
  }

  public record PageBody(List<TicketSummary> content, int page, int size, long total) {}
}

@RestControllerAdvice
class ApiErrors {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<ErrorBody> handle(ApiException ex) {
    return ResponseEntity.status(ex.status()).body(new ErrorBody(Instant.now(), ex.status(), ex.error(), ex.getMessage(), ex.fields()));
  }

  record ErrorBody(Instant timestamp, int status, String error, String message, List<FieldError> fieldErrors) {}
}
