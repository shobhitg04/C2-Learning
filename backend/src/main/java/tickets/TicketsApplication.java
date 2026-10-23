package tickets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import tickets.ai.RagProperties;

@SpringBootApplication
@EnableConfigurationProperties(RagProperties.class)
public class TicketsApplication {
  public static void main(String[] args) {
    SpringApplication.run(TicketsApplication.class, args);
  }
}
