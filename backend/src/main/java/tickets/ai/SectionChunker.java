package tickets.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import tickets.domain.Comment;
import tickets.domain.SectionDocument;
import tickets.domain.Ticket;

public final class SectionChunker {
  private static final Pattern WORD = Pattern.compile("\\S+");
  private static final int LIMIT = 500;

  private SectionChunker() {}

  public static List<SectionDocument> chunk(Ticket ticket) {
    var docs = new ArrayList<SectionDocument>();
    var words = WORD.matcher(ticket.description()).results().map(MatchResult::group).toList();
    if (words.size() <= LIMIT) {
      docs.add(new SectionDocument(section(ticket.id(), "description", null, ticket.description().trim()), "description", null, null));
    } else {
      for (int part = 0; part * LIMIT < words.size(); part++) {
        int from = part * LIMIT;
        int to = Math.min(from + LIMIT, words.size());
        var body = String.join(" ", words.subList(from, to));
        docs.add(new SectionDocument(section(ticket.id(), "description", null, body), "description", null, part + 1));
      }
    }
    var comments = ticket.comments();
    for (int i = 0; i < comments.size(); i++) {
      Comment comment = comments.get(i);
      int index = i + 1;
      var content = "Ticket: " + ticket.id() + "\nSection: comment\nIndex: " + index + "\n" + comment.body().trim();
      docs.add(new SectionDocument(content, "comment", index, null));
    }
    if (ticket.resolutionNotes() != null && !ticket.resolutionNotes().isBlank()) {
      docs.add(new SectionDocument(section(ticket.id(), "resolution", null, ticket.resolutionNotes().trim()), "resolution", null, null));
    }
    return docs;
  }

  private static String section(String id, String name, Integer index, String body) {
    return "Ticket: " + id + "\nSection: " + name + "\n" + body;
  }
}
