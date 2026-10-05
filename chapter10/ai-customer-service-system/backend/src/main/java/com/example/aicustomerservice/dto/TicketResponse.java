package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.SupportTicket;
import com.example.aicustomerservice.entity.TicketSlaSnapshot;
import com.example.aicustomerservice.entity.TicketTimelineEvent;
import com.example.aicustomerservice.service.TicketSlaPolicy;
import java.time.Instant;
import java.util.List;

public record TicketResponse(
    String ticketId,
    String customerId,
    String sessionId,
    String question,
    String answerDraft,
    String intent,
    String status,
    String priority,
    String owner,
    Instant createdAt,
    Instant updatedAt,
    Instant resolvedAt,
    int duplicateCount,
    String slaStatus,
    Instant slaDueAt,
    long ageMinutes,
    long remainingMinutes,
    boolean slaBreached,
    boolean actionable,
    List<TimelineResponse> timeline
) {
    public static TicketResponse from(SupportTicket ticket) {
        return from(ticket, Instant.now());
    }

    public static TicketResponse from(SupportTicket ticket, Instant now) {
        TicketSlaSnapshot sla = new TicketSlaPolicy().evaluate(ticket, now);
        return new TicketResponse(
            ticket.ticketId(),
            ticket.customerId(),
            ticket.sessionId(),
            ticket.question(),
            ticket.answerDraft(),
            ticket.intent(),
            ticket.status().name(),
            ticket.priority().name(),
            ticket.owner(),
            ticket.createdAt(),
            ticket.updatedAt(),
            ticket.resolvedAt(),
            ticket.duplicateCount(),
            sla.status(),
            sla.dueAt(),
            sla.ageMinutes(),
            sla.remainingMinutes(),
            sla.breached(),
            ticket.resolvedAt() == null,
            ticket.timeline().stream().map(TimelineResponse::from).toList()
        );
    }

    public record TimelineResponse(
        Instant occurredAt,
        String type,
        String operator,
        String message
    ) {
        public static TimelineResponse from(TicketTimelineEvent event) {
            return new TimelineResponse(event.occurredAt(), event.type(), event.operator(), event.message());
        }
    }
}
