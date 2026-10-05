package com.example.aicustomerservice.service;

import com.example.aicustomerservice.entity.SupportTicket;
import com.example.aicustomerservice.entity.TicketPriority;
import com.example.aicustomerservice.entity.TicketSlaSnapshot;
import com.example.aicustomerservice.entity.TicketStatus;
import java.time.Duration;
import java.time.Instant;

/**
 * 工单 SLA 策略。
 *
 * 真实客服系统通常会把 SLA 放在规则中心或配置中心。
 * 这里先用代码集中维护，避免 Controller 和前端各自写一套超时判断。
 */
public class TicketSlaPolicy {

    public TicketSlaSnapshot evaluate(SupportTicket ticket, Instant now) {
        Instant dueAt = ticket.createdAt().plus(responseWindow(ticket.priority()));
        Instant endAt = ticket.resolvedAt() == null ? now : ticket.resolvedAt();
        long ageMinutes = Math.max(0, Duration.between(ticket.createdAt(), endAt).toMinutes());
        long remainingMinutes = Duration.between(now, dueAt).toMinutes();

        if (ticket.status() == TicketStatus.RESOLVED) {
            boolean resolvedLate = ticket.resolvedAt() != null && ticket.resolvedAt().isAfter(dueAt);
            return new TicketSlaSnapshot(
                resolvedLate ? "RESOLVED_LATE" : "RESOLVED",
                dueAt,
                ageMinutes,
                remainingMinutes,
                resolvedLate
            );
        }

        if (now.isAfter(dueAt)) {
            return new TicketSlaSnapshot("BREACHED", dueAt, ageMinutes, remainingMinutes, true);
        }
        if (remainingMinutes <= 30) {
            return new TicketSlaSnapshot("DUE_SOON", dueAt, ageMinutes, remainingMinutes, false);
        }
        return new TicketSlaSnapshot("ON_TRACK", dueAt, ageMinutes, remainingMinutes, false);
    }

    private Duration responseWindow(TicketPriority priority) {
        return switch (priority) {
            case URGENT -> Duration.ofMinutes(30);
            case HIGH -> Duration.ofHours(2);
            case NORMAL -> Duration.ofHours(8);
            case LOW -> Duration.ofHours(24);
        };
    }
}
