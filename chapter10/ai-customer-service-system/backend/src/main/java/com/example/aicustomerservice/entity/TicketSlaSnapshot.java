package com.example.aicustomerservice.entity;

import java.time.Instant;

public record TicketSlaSnapshot(
    String status,
    Instant dueAt,
    long ageMinutes,
    long remainingMinutes,
    boolean breached
) {
}
