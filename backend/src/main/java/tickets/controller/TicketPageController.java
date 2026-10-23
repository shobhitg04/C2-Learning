package tickets.controller;

import java.util.Arrays;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tickets.domain.Status;
import tickets.domain.StatusPolicy;
import tickets.domain.Ticket;
import tickets.service.TicketService;
import tickets.service.TicketService.ApiException;

@Controller
public class TicketPageController {
  private final TicketService tickets;

  public TicketPageController(TicketService tickets) {
    this.tickets = tickets;
  }

  @GetMapping("/tickets")
  public String list(@RequestParam(required = false) String keyword, @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, Model model) {
    var found = tickets.list(keyword, blank(status) ? null : status, page, 20);
    model.addAttribute("tickets", found.content());
    model.addAttribute("keyword", keyword == null ? "" : keyword);
    model.addAttribute("status", status == null ? "" : status);
    model.addAttribute("page", page);
    model.addAttribute("total", found.total());
    return "tickets/list";
  }

  @GetMapping("/tickets/new")
  public String createForm() {
    return "tickets/form";
  }

  @GetMapping("/tickets/{id}")
  public String detail(@PathVariable String id, Model model) {
    model.addAttribute("ticket", tickets.get(id));
    return "tickets/detail";
  }

  @GetMapping("/tickets/{id}/edit")
  public String editForm(@PathVariable String id, Model model) {
    Ticket ticket = tickets.get(id);
    model.addAttribute("ticket", ticket);
    model.addAttribute("targets", Arrays.stream(Status.values()).filter(to -> StatusPolicy.allowed(ticket.status(), to)).toList());
    return "tickets/edit";
  }

  @PostMapping("/tickets/{id}/comments")
  public String comment(@PathVariable String id, @RequestParam String body) {
    tickets.addComment(id, body);
    return "redirect:/tickets/" + id;
  }

  @PostMapping("/tickets/{id}/edit")
  public String edit(
      @PathVariable String id,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String assignee,
      @RequestParam(required = false) String resolutionNotes,
      @RequestParam(defaultValue = "false") boolean clearNotes,
      Model model) {
    Ticket current = tickets.get(id);
    boolean statusSet = status != null && !status.equals("CURRENT");
    boolean notesSet = clearNotes || (resolutionNotes != null && !resolutionNotes.equals(current.resolutionNotes() == null ? "" : current.resolutionNotes()));
    try {
      tickets.patch(id, statusSet, status, true, blank(assignee) ? null : assignee, notesSet, clearNotes ? null : resolutionNotes, true);
      return "redirect:/tickets/" + id;
    } catch (ApiException ex) {
      model.addAttribute("banner", ex.getMessage());
      model.addAttribute("ticket", current);
      model.addAttribute("targets", Arrays.stream(Status.values()).filter(to -> StatusPolicy.allowed(current.status(), to)).toList());
      return "tickets/edit";
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
