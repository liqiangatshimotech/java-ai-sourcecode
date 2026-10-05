package com.example.aicustomerservice.entity;

import java.time.Instant;

public record TicketTimelineEvent(
    Instant occurredAt,
    String type,
    String operator,
    String message
) {
}
