package tickets.domain;

import java.util.List;

public record Ticket(
    String id,
    String description,
    Status status,
    String priority,
    String assignee,
    String category,
    List<Comment> comments,
    String resolutionNotes) {}
