package tickets.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tickets.ai.AskService;

@RestController
public class AskController {
  private final AskService ask;

  public AskController(AskService ask) {
    this.ask = ask;
  }

  @PostMapping("/api/ai/ask")
  public AskService.Answer ask(@RequestBody AskRequest body) {
    return ask.ask(body == null ? null : body.question());
  }

  public record AskRequest(String question) {}
}
