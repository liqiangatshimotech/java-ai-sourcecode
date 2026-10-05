package com.example.enterpriseopsagent.domain;

import java.time.Instant;

/**
 * 排障事件时间线。
 *
 * 企业排障系统一定要能回放“谁在什么时候做了什么”，否则后续复盘和审计都无从谈起。
 */
public record IncidentTimelineEvent(
    Instant occurredAt,
    String type,
    String operator,
    String message
) {
    public IncidentTimelineEvent {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("timeline event type 不能为空");
        }
        if (operator == null || operator.isBlank()) {
            operator = "system";
        }
        if (message == null) {
            message = "";
        }
    }
}
