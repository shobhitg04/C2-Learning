package tickets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TicketPageTest extends PgTest {
  @Autowired MockMvc mvc;

  @Test
  void should_showKeyword_when_listIsOpened() throws Exception {
    mvc.perform(get("/tickets")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"keyword\"")));
  }

  @Test
  void should_blockBlankDescription_when_createPageLoads() throws Exception {
    mvc.perform(get("/tickets/new"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"description\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("value.trim() === ''")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("must not be blank")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("err.message")));
  }

  @Test
  void should_showServerMessage_when_createFails() throws Exception {
    mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content("{\"description\":\" \",\"priority\":\"HIGH\",\"category\":\"billing\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("must not be blank")));
  }
}
