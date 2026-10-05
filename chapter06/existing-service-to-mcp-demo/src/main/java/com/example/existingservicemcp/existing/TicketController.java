package com.example.existingservicemcp.existing;

import com.example.existingservicemcp.security.DemoUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TicketController {

    private final DemoUserContext userContext;
    private final SupportTicketService supportTicketService;

    public TicketController(DemoUserContext userContext, SupportTicketService supportTicketService) {
        this.userContext = userContext;
        this.supportTicketService = supportTicketService;
    }

    @GetMapping("/tickets/{ticketId}")
    public TicketView getTicket(@PathVariable String ticketId) {
        return supportTicketService.getTicket(userContext.currentUser(), ticketId);
    }

    @PostMapping("/tickets/{ticketId}/reply-draft")
    public ReplyDraft draftReply(@PathVariable String ticketId, @RequestParam(defaultValue = "warm") String tone) {
        return supportTicketService.draftReply(userContext.currentUser(), ticketId, tone);
    }
}
