package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.TicketDashboard;
import java.time.Instant;

public record TicketDashboardResponse(
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
    public static TicketDashboardResponse from(TicketDashboard dashboard) {
        return new TicketDashboardResponse(
            dashboard.total(),
            dashboard.openCount(),
            dashboard.newCount(),
            dashboard.assignedCount(),
            dashboard.resolvedCount(),
            dashboard.urgentCount(),
            dashboard.breachedCount(),
            dashboard.dueSoonCount(),
            dashboard.duplicateHitCount(),
            dashboard.generatedAt()
        );
    }
}
