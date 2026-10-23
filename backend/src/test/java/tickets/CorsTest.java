package tickets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CorsTest extends PgTest {
  @Autowired MockMvc mvc;

  @Test
  void should_allow_when_originIsLocalFrontend() throws Exception {
    mvc.perform(options("/api/tickets")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Content-Type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
  }

  @Test
  void should_reject_when_originIsNotLocalFrontend() throws Exception {
    mvc.perform(options("/api/tickets")
            .header("Origin", "http://localhost:3001")
            .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }
}
