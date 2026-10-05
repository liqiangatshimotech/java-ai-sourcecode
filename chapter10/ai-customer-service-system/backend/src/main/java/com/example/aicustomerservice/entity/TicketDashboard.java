package com.example.aicustomerservice.entity;

import java.time.Instant;

public record TicketDashboard(
    int total,
    int openCount,
    int newCount,
    int assignedCount,
    int resolvedCount,
    int urgentCount,
    int breachedCount,
    int dueSoonCount,
    int duplicateHitCount,
    Instant generatedAt
) {
}
