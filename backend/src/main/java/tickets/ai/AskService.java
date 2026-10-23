package tickets.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tickets.repository.TicketRepository;
import tickets.service.TicketService.ApiException;
import tickets.service.TicketService.FieldError;

@Service
public class AskService {
  static final String NO_MATCH = "No relevant tickets found.";
  static final String SYSTEM = """
      You answer questions about support tickets.
      Answer only from the supplied context.
      Cite each supporting ticket as [TKT-1001], using that ticket's real id from the context.
      When the context is empty or irrelevant, reply exactly: No relevant tickets found.
      Never invent ticket IDs.
      Do not use general knowledge.
      Do not create a ticket, add a comment, change status, change assignee, or write resolution notes.""".strip();

  private static final Logger log = LoggerFactory.getLogger(AskService.class);
  private static final Pattern TICKET_ID = Pattern.compile("TKT-[0-9]+");

  private final TicketRepository tickets;
  private final JdbcTemplate jdbc;
  private final Embedder embedder;
  private final RagProperties rag;
  private final ChatClient chat;

  public AskService(TicketRepository tickets, JdbcTemplate jdbc, Embedder embedder, RagProperties rag, ChatClient.Builder chat) {
    this.tickets = tickets;
    this.jdbc = jdbc;
    this.embedder = embedder;
    this.rag = rag;
    this.chat = chat.build();
  }

  public Answer ask(String question) {
    if (question == null || question.isBlank()) {
      throw new ApiException(400, "Bad Request", "Validation failed", List.of(new FieldError("question", "must not be blank")));
    }
    question = question.trim();
    float[] vector;
    try {
      vector = embedder.embed(question);
    } catch (RuntimeException ex) {
      throw unavailable();
    }
    var hits = search(vector);
    if (hits.isEmpty()) {
      return noMatch();
    }
    String text;
    try {
      text = chat.prompt().system(SYSTEM).user(userPrompt(question, hits)).call().content();
    } catch (RuntimeException ex) {
      throw unavailable();
    }
    if (text == null || text.isBlank()) {
      throw unavailable();
    }
    text = text.trim();
    if (text.equals(NO_MATCH)) {
      return noMatch();
    }
    var allowed = hits.stream().map(Hit::ticketId).distinct().toList();
    var matcher = TICKET_ID.matcher(text);
    while (matcher.find()) {
      if (!allowed.contains(matcher.group())) {
        log.warn("Discarded answer that cited {}", matcher.group());
        return noMatch();
      }
    }
    return new Answer(text, sources(hits), false);
  }

  private List<Hit> search(float[] vector) {
    var rows = jdbc.query(
        """
        SELECT content, metadata->>'ticketId' AS ticket_id, embedding <=> ?::vector AS distance
        FROM vector_store
        ORDER BY distance
        LIMIT ?
        """,
        (rs, row) -> new Hit(
            rs.getString("ticket_id"),
            rs.getString("content"),
            1 - rs.getDouble("distance")),
        TicketIndexer.vector(vector),
        rag.topK());
    var best = new LinkedHashMap<String, Hit>();
    var texts = new LinkedHashMap<String, List<String>>();
    for (Hit hit : rows) {
      if (hit.ticketId() == null || !Double.isFinite(hit.similarity()) || hit.similarity() < rag.similarityThreshold()) {
        continue;
      }
      texts.computeIfAbsent(hit.ticketId(), id -> new ArrayList<>()).add(hit.content());
      best.merge(hit.ticketId(), hit, (left, right) -> left.similarity() >= right.similarity() ? left : right);
    }
    return best.values().stream().map(hit -> hit.with(texts.get(hit.ticketId()))).toList();
  }

  private List<Source> sources(List<Hit> hits) {
    return hits.stream()
        .map(hit -> new Source(hit.ticketId(), title(hit.ticketId()), hit.similarity()))
        .sorted(Comparator.comparingDouble(Source::similarity).reversed().thenComparing(Source::ticketId))
        .toList();
  }

  private String title(String ticketId) {
    return tickets.find(ticketId).map(ticket -> ticket.description()).orElse("");
  }

  private static String userPrompt(String question, List<Hit> hits) {
    var context = new StringBuilder();
    for (Hit hit : hits) {
      for (String chunk : hit.chunks()) {
        context.append(chunk).append('\n');
      }
    }
    return "Question: " + question + "\n\nContext:\n" + context;
  }

  private static Answer noMatch() {
    return new Answer(NO_MATCH, List.of(), true);
  }

  private static ApiException unavailable() {
    return new ApiException(503, "Service Unavailable", "Question could not be answered", List.of());
  }

  public record Answer(String answer, List<Source> sources, boolean noRelevantTickets) {}

  public record Source(String ticketId, String title, double similarity) {}

  private record Hit(String ticketId, String content, double similarity, List<String> chunks) {
    Hit(String ticketId, String content, double similarity) {
      this(ticketId, content, similarity, List.of());
    }

    Hit with(List<String> chunks) {
      return new Hit(ticketId, content, similarity, chunks);
    }
  }
}
