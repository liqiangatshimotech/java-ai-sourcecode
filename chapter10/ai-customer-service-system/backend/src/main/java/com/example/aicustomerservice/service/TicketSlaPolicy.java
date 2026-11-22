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
        // 截止时间只由创建时间和优先级窗口计算，Controller 与前端不重复写规则。
        Instant dueAt = ticket.createdAt().plus(responseWindow(ticket.priority()));

        // 工单已持续时长：未解决算到当前时间，已解决算到真实解决时间。
        Instant endAt = ticket.resolvedAt() == null ? now : ticket.resolvedAt();
        long ageMinutes = Math.max(0, Duration.between(ticket.createdAt(), endAt).toMinutes());
        long remainingMinutes = Duration.between(now, dueAt).toMinutes();

        if (ticket.status() == TicketStatus.RESOLVED) {
            // 已解决不等于按时解决，RESOLVED_LATE 会保留历史违约事实。
            boolean resolvedLate = ticket.resolvedAt() != null
                && ticket.resolvedAt().isAfter(dueAt);
            return new TicketSlaSnapshot(
                resolvedLate ? "RESOLVED_LATE" : "RESOLVED",
                dueAt,
                ageMinutes,
                remainingMinutes,
                resolvedLate
            );
        }

        // 未解决且已经超过截止时间时，明确标记 BREACHED。
        if (now.isAfter(dueAt)) {
            return new TicketSlaSnapshot("BREACHED", dueAt, ageMinutes, remainingMinutes, true);
        }

        // 剩余 30 分钟以内进入预警区，驾驶舱可以提前提示，而不是等超时后再处理。
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
