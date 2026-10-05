package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.ConversationDashboard;
import java.time.Instant;
import java.util.Map;

/** 会话运营看板响应。 */
public record ConversationDashboardResponse(
    int totalCount,
    int openCount,
    int waitingCount,
    int inProgressCount,
    int waitingCustomerCount,
    int waitingInternalCount,
    int resolvedCount,
    int resolvedTodayCount,
    int unassignedCount,
    int urgentCount,
    int slaRiskCount,
    long averageWaitingMinutes,
    long averageWaitSeconds,
    long averageFirstResponseSeconds,
    double aiAssistRate,
    Double csatScore,
    Map<String, Integer> channelCounts,
    Map<String, Integer> statusCounts,
    Instant updatedAt
) {
    public static ConversationDashboardResponse from(ConversationDashboard dashboard) {
        return new ConversationDashboardResponse(
            dashboard.totalCount(), dashboard.openCount(), dashboard.waitingCount(),
            dashboard.inProgressCount(), dashboard.waitingCustomerCount(), dashboard.waitingInternalCount(),
            dashboard.resolvedCount(), dashboard.resolvedTodayCount(), dashboard.unassignedCount(),
            dashboard.urgentCount(), dashboard.slaRiskCount(), dashboard.averageWaitingMinutes(),
            dashboard.averageWaitSeconds(), dashboard.averageFirstResponseSeconds(), dashboard.aiAssistRate(),
            dashboard.csatScore(), dashboard.channelCounts(), dashboard.statusCounts(), dashboard.updatedAt()
        );
    }
}
