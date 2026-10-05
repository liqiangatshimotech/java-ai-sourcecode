package com.example.aicustomerservice.controller;

import com.example.aicustomerservice.dto.TicketAssignRequest;
import com.example.aicustomerservice.dto.TicketCloseRequest;
import com.example.aicustomerservice.dto.TicketDashboardResponse;
import com.example.aicustomerservice.dto.TicketNoteRequest;
import com.example.aicustomerservice.dto.TicketResponse;
import com.example.aicustomerservice.entity.TicketStatus;
import com.example.aicustomerservice.service.TicketService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final Clock clock;

    public TicketController(TicketService ticketService, Clock clock) {
        this.ticketService = ticketService;
        this.clock = clock;
    }

    @GetMapping("/{ticketId}")
    public TicketResponse get(@PathVariable String ticketId) {
        return TicketResponse.from(ticketService.get(ticketId), clock.instant());
    }

    @GetMapping("/dashboard")
    public TicketDashboardResponse dashboard() {
        return TicketDashboardResponse.from(ticketService.dashboard());
    }

    @GetMapping
    public List<TicketResponse> search(
        @RequestParam(required = false) TicketStatus status,
        @RequestParam(required = false) String customerId
    ) {
        Instant now = clock.instant();
        return ticketService.search(status, customerId).stream()
            .map(ticket -> TicketResponse.from(ticket, now))
            .toList();
    }

    @PostMapping("/{ticketId}/assign")
    public TicketResponse assign(
        @PathVariable String ticketId,
        @Valid @RequestBody TicketAssignRequest request
    ) {
        return TicketResponse.from(ticketService.assign(ticketId, request.owner()), clock.instant());
    }

    @PostMapping("/{ticketId}/notes")
    public TicketResponse addNote(
        @PathVariable String ticketId,
        @Valid @RequestBody TicketNoteRequest request
    ) {
        return TicketResponse.from(
            ticketService.addNote(ticketId, com.example.aicustomerservice.security.SupportIdentity.operator(request.operator()), request.message()),
            clock.instant()
        );
    }

    @PostMapping("/{ticketId}/close")
    public TicketResponse close(
        @PathVariable String ticketId,
        @Valid @RequestBody TicketCloseRequest request
    ) {
        return TicketResponse.from(
            ticketService.close(ticketId, com.example.aicustomerservice.security.SupportIdentity.operator(request.operator()), request.summary()),
            clock.instant()
        );
    }
}
