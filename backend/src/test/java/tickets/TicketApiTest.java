package tickets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tickets.ai.Embedder;

@SpringBootTest
@AutoConfigureMockMvc
class TicketApiTest extends PgTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean Embedder embedder;

  @BeforeEach
  void reset() {
    given(embedder.embed(any())).willReturn(new float[768]);
    jdbc.update("DELETE FROM vector_store");
    jdbc.update("TRUNCATE ticket CASCADE");
    jdbc.execute("SELECT setval('ticket_id_seq', 1001, false)");
  }

  @Test
  void should_create_when_fieldsAreValid() throws Exception {
    mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(body("Invoice export fails", "HIGH", "billing")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("TKT-1001"))
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.assignee").isEmpty());
  }

  @Test
  void should_reject_when_descriptionIsBlank() throws Exception {
    mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(body("  ", "HIGH", "billing")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[?(@.field == 'description')].message").value("must not be blank"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket", Long.class)).isZero();
  }

  @Test
  void should_storeHigh_when_priorityIsLowercase() throws Exception {
    mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(body("A ticket", "high", "billing")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.priority").value("HIGH"));
  }

  @Test
  void should_reject_when_priorityIsUnknown() throws Exception {
    mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(body("A ticket", "URGENT", "billing")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[?(@.field == 'priority')].message").value("must be HIGH, MEDIUM, or LOW"));
  }

  @Test
  void should_return404_when_ticketIsMissing() throws Exception {
    mvc.perform(get("/api/tickets/TKT-9999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Ticket TKT-9999 not found"))
        .andExpect(jsonPath("$.fieldErrors").isEmpty());
  }

  @Test
  void should_reject_when_idIsMalformed() throws Exception {
    mvc.perform(get("/api/tickets/nope"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[?(@.field == 'id')].message").value("must match TKT- and digits"));
  }

  @Test
  void should_reject_when_patchIsEmpty() throws Exception {
    create();
    mvc.perform(patch("/api/tickets/TKT-1001").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[?(@.field == 'body')].message").value("must change at least one field"));
  }

  @Test
  void should_returnOneRow_when_pageSizeIsOne() throws Exception {
    create();
    create("Second", "LOW", "auth");
    mvc.perform(get("/api/tickets").param("page", "0").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.total").value(2));
  }

  @Test
  void should_filter_when_keywordAndStatusMatch() throws Exception {
    create();
    create("Users are logged out", "MEDIUM", "auth");
    mvc.perform(patch("/api/tickets/TKT-1002").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\"}"));
    mvc.perform(get("/api/tickets").param("keyword", "export").param("status", "OPEN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.content[0].id").value("TKT-1001"));
  }

  @Test
  void should_returnEmptyPage_when_pageIsPastTheEnd() throws Exception {
    create();
    mvc.perform(get("/api/tickets").param("page", "5").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.total").value(1));
  }

  @Test
  void should_reject_when_pageIsNotAnInteger() throws Exception {
    mvc.perform(get("/api/tickets").param("page", "nope"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[?(@.field == 'page')].message").exists());
  }

  @Test
  void should_leaveChunks_when_statusDoesNotChange() throws Exception {
    create();
    List<String> before = chunkIds();
    mvc.perform(patch("/api/tickets/TKT-1001").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Status cannot change from OPEN to OPEN"));
    assertThat(chunkIds()).isEqualTo(before);
  }

  @Test
  void should_reindex_when_resolutionNotesChange() throws Exception {
    create();
    mvc.perform(patch("/api/tickets/TKT-1001").contentType(MediaType.APPLICATION_JSON).content("{\"resolutionNotes\":\"old notes\"}"))
        .andExpect(status().isOk());
    assertThat(textOf("TKT-1001")).contains("old notes");
    mvc.perform(patch("/api/tickets/TKT-1001").contentType(MediaType.APPLICATION_JSON).content("{\"resolutionNotes\":\"new notes\"}"))
        .andExpect(status().isOk());
    assertThat(textOf("TKT-1001")).doesNotContain("old notes").contains("new notes");
  }

  @Test
  void should_moveToInProgress_when_openTicketStartsWork() throws Exception {
    create();
    mvc.perform(patch("/api/tickets/TKT-1001").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
  }

  @Test
  void should_keepNotes_when_resolvedTicketCloses() throws Exception {
    create();
    mvc.perform(patch("/api/tickets/TKT-1001").contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"RESOLVED\",\"resolutionNotes\":\"Raised the session timeout to 8 hours.\"}"));
    mvc.perform(patch("/api/tickets/TKT-1001").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"))
        .andExpect(jsonPath("$.resolutionNotes").value("Raised the session timeout to 8 hours."));
  }

  private void create() throws Exception {
    create("Invoice export fails", "HIGH", "billing");
  }

  private void create(String description, String priority, String category) throws Exception {
    mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(body(description, priority, category)))
        .andExpect(status().isCreated());
  }

  private static String body(String description, String priority, String category) {
    return "{\"description\":\"" + description + "\",\"priority\":\"" + priority + "\",\"category\":\"" + category + "\"}";
  }

  private List<String> chunkIds() {
    return jdbc.queryForList("SELECT id::text FROM vector_store WHERE metadata->>'ticketId' = 'TKT-1001' ORDER BY 1", String.class);
  }

  private String textOf(String id) {
    return String.join("\n", jdbc.queryForList("SELECT content FROM vector_store WHERE metadata->>'ticketId' = ?", String.class, id));
  }
}
