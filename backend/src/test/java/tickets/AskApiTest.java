package tickets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tickets.ai.AskService;
import tickets.ai.Embedder;
import tickets.ai.TicketIndexer;

@SpringBootTest
@AutoConfigureMockMvc
class AskApiTest extends PgTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Environment env;
  @MockitoBean Embedder embedder;
  @MockitoBean ChatModel chatModel;

  @BeforeEach
  void reset() {
    jdbc.update("DELETE FROM vector_store");
    jdbc.update("TRUNCATE ticket CASCADE");
    jdbc.execute("SELECT setval('ticket_id_seq', 1001, false)");
  }

  @Test
  void should_reject_when_questionIsBlank() throws Exception {
    mvc.perform(post("/api/ai/ask").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"  \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[?(@.field == 'question')].message").value("must not be blank"));
    mvc.perform(post("/api/ai/ask").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[?(@.field == 'question')].message").value("must not be blank"));
  }

  @Test
  void should_skipChat_when_nothingPassesThreshold() throws Exception {
    ticket("TKT-1001", "Invoice export fails for quarterly reports.");
    chunk("TKT-1001", "far", axis(1));
    given(embedder.embed(any())).willReturn(axis(0));

    mvc.perform(post("/api/ai/ask").contentType(MediaType.APPLICATION_JSON).content(question("How do I reset a VPN password?")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answer").value("No relevant tickets found."))
        .andExpect(jsonPath("$.sources").isEmpty())
        .andExpect(jsonPath("$.noRelevantTickets").value(true));
    verify(chatModel, never()).call(any(Prompt.class));
  }

  @Test
  void should_keepHighestScore_when_twoChunksMatchOneTicket() throws Exception {
    ticket("TKT-1001", "Invoice export fails for quarterly reports.");
    chunk("TKT-1001", "near", axis(0));
    chunk("TKT-1001", "also", angled());
    chunk("TKT-1002", "far", axis(1));
    given(embedder.embed(any())).willReturn(axis(0));
    reply("Exports fail. [TKT-1001]");

    mvc.perform(post("/api/ai/ask").contentType(MediaType.APPLICATION_JSON).content(question("Why do quarterly invoice exports fail?")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sources.length()").value(1))
        .andExpect(jsonPath("$.sources[0].ticketId").value("TKT-1001"))
        .andExpect(jsonPath("$.sources[0].title").value("Invoice export fails for quarterly reports."))
        .andExpect(jsonPath("$.sources[0].similarity", closeTo(1.0, 0.001)));
  }

  @Test
  void should_returnAnswer_when_modelCitesRetrievedTicket() throws Exception {
    ticket("TKT-1001", "Invoice export fails for quarterly reports.");
    ticket("TKT-1002", "Users are logged out during the day.");
    chunk("TKT-1001", "quarterly export", axis(0));
    chunk("TKT-1002", "logout", angled());
    given(embedder.embed(any())).willReturn(axis(0));
    reply("Exports fail. [TKT-1001]");
    long before = count();

    mvc.perform(post("/api/ai/ask").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Why do exports fail?\",\"extra\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answer").value("Exports fail. [TKT-1001]"))
        .andExpect(jsonPath("$.noRelevantTickets").value(false))
        .andExpect(jsonPath("$.sources.length()").value(2))
        .andExpect(jsonPath("$.sources[0].ticketId").value("TKT-1001"))
        .andExpect(jsonPath("$.sources[1].ticketId").value("TKT-1002"));
    assertThat(count()).isEqualTo(before);
    assertThat(env.getProperty("spring.ai.ollama.chat.options.model")).isEqualTo("llama3.1");

    var captor = ArgumentCaptor.forClass(Prompt.class);
    verify(chatModel).call(captor.capture());
    assertThat(captor.getValue().getInstructions()).anyMatch(message -> message.getText().startsWith("You answer questions about support tickets.")
        && message.getText().contains("Never invent ticket IDs."));
    assertThat(captor.getValue().getInstructions()).anyMatch(message -> message.getText().contains("quarterly export"));
  }

  @Test
  void should_discardAnswer_when_modelCitesUnknownTicket() throws Exception {
    ticket("TKT-1001", "Invoice export fails for quarterly reports.");
    chunk("TKT-1001", "near", axis(0));
    given(embedder.embed(any())).willReturn(axis(0));
    reply("See [TKT-9999].");
    var warnings = warnings();

    mvc.perform(post("/api/ai/ask").contentType(MediaType.APPLICATION_JSON).content(question("Why do exports fail?")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answer").value("No relevant tickets found."))
        .andExpect(jsonPath("$.sources").isEmpty())
        .andExpect(jsonPath("$.noRelevantTickets").value(true));
    assertThat(warnings.list).anyMatch(event -> event.getLevel() == Level.WARN && event.getFormattedMessage().contains("TKT-9999"));
    ((Logger) LoggerFactory.getLogger(AskService.class)).detachAppender(warnings);
  }

  @Test
  void should_returnNoMatch_when_modelDeclines() throws Exception {
    ticket("TKT-1001", "Invoice export fails for quarterly reports.");
    chunk("TKT-1001", "near", axis(0));
    given(embedder.embed(any())).willReturn(axis(0));
    reply("No relevant tickets found.");
    var warnings = warnings();

    mvc.perform(post("/api/ai/ask").contentType(MediaType.APPLICATION_JSON).content(question("How do I reset a VPN password?")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answer").value("No relevant tickets found."))
        .andExpect(jsonPath("$.sources").isEmpty())
        .andExpect(jsonPath("$.noRelevantTickets").value(true));
    assertThat(warnings.list).noneMatch(event -> event.getLevel() == Level.WARN);
    ((Logger) LoggerFactory.getLogger(AskService.class)).detachAppender(warnings);
  }

  @Test
  void should_return503_when_embedFails() throws Exception {
    ticket("TKT-1001", "Invoice export fails for quarterly reports.");
    given(embedder.embed(any())).willThrow(new IllegalStateException("down"));

    mvc.perform(post("/api/ai/ask").contentType(MediaType.APPLICATION_JSON).content(question("Why do exports fail?")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("Service Unavailable"))
        .andExpect(jsonPath("$.message").value("Question could not be answered"))
        .andExpect(jsonPath("$.fieldErrors").isEmpty());
    assertThat(count()).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT status FROM ticket WHERE id = 'TKT-1001'", String.class)).isEqualTo("OPEN");
  }

  @Test
  void should_return503_when_chatFails() throws Exception {
    ticket("TKT-1001", "Invoice export fails for quarterly reports.");
    chunk("TKT-1001", "near", axis(0));
    given(embedder.embed(any())).willReturn(axis(0));
    given(chatModel.call(any(Prompt.class))).willThrow(new IllegalStateException("down"));

    mvc.perform(post("/api/ai/ask").contentType(MediaType.APPLICATION_JSON).content(question("Why do exports fail?")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("Question could not be answered"))
        .andExpect(jsonPath("$.fieldErrors").isEmpty());
    assertThat(count()).isEqualTo(1);
  }

  private void ticket(String id, String description) {
    jdbc.update(
        "INSERT INTO ticket (id, description, status, priority, category) VALUES (?, ?, 'OPEN', 'HIGH', 'billing')",
        id, description);
  }

  private void chunk(String ticketId, String content, float[] embedding) {
    jdbc.update(
        "INSERT INTO vector_store (content, metadata, embedding) VALUES (?, ?::json, ?::vector)",
        content,
        "{\"ticketId\":\"" + ticketId + "\"}",
        TicketIndexer.vector(embedding));
  }

  private void reply(String text) {
    given(chatModel.call(any(Prompt.class))).willReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(text)))));
  }

  private long count() {
    return jdbc.queryForObject("SELECT count(*) FROM ticket", Long.class);
  }

  private static ListAppender<ILoggingEvent> warnings() {
    var logger = (Logger) LoggerFactory.getLogger(AskService.class);
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    return appender;
  }

  private static String question(String question) {
    return "{\"question\":\"" + question + "\"}";
  }

  static float[] axis(int index) {
    var values = new float[768];
    values[index] = 1f;
    return values;
  }

  static float[] angled() {
    var values = new float[768];
    values[0] = 0.8f;
    values[1] = 0.6f;
    return values;
  }
}

@SpringBootTest(properties = "app.rag.top-k=1")
@AutoConfigureMockMvc
class AskTopKTest extends PgTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean Embedder embedder;
  @MockitoBean ChatModel chatModel;

  @BeforeEach
  void reset() {
    jdbc.update("DELETE FROM vector_store");
    jdbc.update("TRUNCATE ticket CASCADE");
  }

  @Test
  void should_keepNearestOnly_when_topKIsOverridden() throws Exception {
    insert("TKT-1001", "Invoice export fails for quarterly reports.");
    insert("TKT-1002", "Users are logged out during the day.");
    store("TKT-1001", AskApiTest.axis(0));
    store("TKT-1002", AskApiTest.angled());
    given(embedder.embed(any())).willReturn(AskApiTest.axis(0));
    given(chatModel.call(any(Prompt.class))).willReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Exports fail. [TKT-1001]")))));

    mvc.perform(post("/api/ai/ask").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Why do exports fail?\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sources.length()").value(1))
        .andExpect(jsonPath("$.sources[0].ticketId").value("TKT-1001"));
  }

  private void insert(String id, String description) {
    jdbc.update(
        "INSERT INTO ticket (id, description, status, priority, category) VALUES (?, ?, 'OPEN', 'HIGH', 'billing')",
        id, description);
  }

  private void store(String ticketId, float[] embedding) {
    jdbc.update(
        "INSERT INTO vector_store (content, metadata, embedding) VALUES (?, ?::json, ?::vector)",
        ticketId,
        "{\"ticketId\":\"" + ticketId + "\"}",
        TicketIndexer.vector(embedding));
  }
}

@SpringBootTest(properties = "app.rag.similarity-threshold=0.95")
@AutoConfigureMockMvc
class AskThresholdTest extends PgTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean Embedder embedder;
  @MockitoBean ChatModel chatModel;

  @BeforeEach
  void reset() {
    jdbc.update("DELETE FROM vector_store");
    jdbc.update("TRUNCATE ticket CASCADE");
  }

  @Test
  void should_dropNearMiss_when_thresholdIsOverridden() throws Exception {
    insert("TKT-1001", "Invoice export fails for quarterly reports.");
    insert("TKT-1002", "Users are logged out during the day.");
    store("TKT-1001", AskApiTest.axis(0));
    store("TKT-1002", AskApiTest.angled());
    given(embedder.embed(any())).willReturn(AskApiTest.axis(0));
    given(chatModel.call(any(Prompt.class))).willReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Exports fail. [TKT-1001]")))));

    mvc.perform(post("/api/ai/ask").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Why do exports fail?\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sources.length()").value(1))
        .andExpect(jsonPath("$.sources[0].ticketId").value("TKT-1001"));
  }

  private void insert(String id, String description) {
    jdbc.update(
        "INSERT INTO ticket (id, description, status, priority, category) VALUES (?, ?, 'OPEN', 'HIGH', 'billing')",
        id, description);
  }

  private void store(String ticketId, float[] embedding) {
    jdbc.update(
        "INSERT INTO vector_store (content, metadata, embedding) VALUES (?, ?::json, ?::vector)",
        ticketId,
        "{\"ticketId\":\"" + ticketId + "\"}",
        TicketIndexer.vector(embedding));
  }
}
