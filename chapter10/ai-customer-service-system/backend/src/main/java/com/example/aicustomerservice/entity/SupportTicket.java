package com.example.aicustomerservice.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record SupportTicket(
    String ticketId,
    String customerId,
    String sessionId,
    String question,
    String answerDraft,
    String intent,
    TicketStatus status,
    TicketPriority priority,
    String owner,
    String idempotencyKey,
    Instant createdAt,
    Instant updatedAt,
    Instant resolvedAt,
    int duplicateCount,
    List<TicketTimelineEvent> timeline
) {
    public SupportTicket {
        timeline = List.copyOf(timeline);
    }

    public static SupportTicket create(
        String ticketId,
        String customerId,
        String sessionId,
        String question,
        String answerDraft,
        String intent,
        TicketPriority priority,
        String idempotencyKey,
        Instant now
    ) {
        return new SupportTicket(
            ticketId,
            customerId,
            sessionId,
            question,
            answerDraft,
            intent,
            TicketStatus.NEW,
            priority,
            "",
            idempotencyKey,
            now,
            now,
            null,
            0,
            List.of(new TicketTimelineEvent(now, "CREATED", "ai-orchestrator", "AI 已创建待处理工单。"))
        );
    }

    public SupportTicket markDuplicate(Instant now) {
        List<TicketTimelineEvent> events = new ArrayList<>(timeline);
        boolean reopenResolvedTicket = status == TicketStatus.RESOLVED;
        events.add(new TicketTimelineEvent(
            now,
            reopenResolvedTicket ? "REOPENED_BY_DUPLICATE" : "DUPLICATE_HIT",
            "ai-orchestrator",
            reopenResolvedTicket ? "关闭工单再次命中幂等键，重新打开原工单继续处理。" : "重复问题命中幂等键，复用已有工单。"
        ));
        return new SupportTicket(
            ticketId,
            customerId,
            sessionId,
            question,
            answerDraft,
            intent,
            reopenResolvedTicket ? TicketStatus.NEW : status,
            priority,
            reopenResolvedTicket ? "" : owner,
            idempotencyKey,
            createdAt,
            now,
            reopenResolvedTicket ? null : resolvedAt,
            duplicateCount + 1,
            events
        );
    }

    public SupportTicket assign(String owner, Instant now) {
        List<TicketTimelineEvent> events = new ArrayList<>(timeline);
        events.add(new TicketTimelineEvent(now, "ASSIGNED", owner, "工单已分配给 " + owner + "。"));
        return new SupportTicket(
            ticketId,
            customerId,
            sessionId,
            question,
            answerDraft,
            intent,
            TicketStatus.ASSIGNED,
            priority,
            owner,
            idempotencyKey,
            createdAt,
            now,
            resolvedAt,
            duplicateCount,
            events
        );
    }

    public SupportTicket addNote(String operator, String message, Instant now) {
        List<TicketTimelineEvent> events = new ArrayList<>(timeline);
        events.add(new TicketTimelineEvent(now, "NOTE_ADDED", operator, message));
        return new SupportTicket(
            ticketId,
            customerId,
            sessionId,
            question,
            answerDraft,
            intent,
            status,
            priority,
            owner,
            idempotencyKey,
            createdAt,
            now,
            resolvedAt,
            duplicateCount,
            events
        );
    }

    public SupportTicket close(String operator, String summary, Instant now) {
        List<TicketTimelineEvent> events = new ArrayList<>(timeline);
        events.add(new TicketTimelineEvent(now, "RESOLVED", operator, summary));
        return new SupportTicket(
            ticketId,
            customerId,
            sessionId,
            question,
            answerDraft,
            intent,
            TicketStatus.RESOLVED,
            priority,
            owner,
            idempotencyKey,
            createdAt,
            now,
            now,
            duplicateCount,
            events
        );
    }
}
