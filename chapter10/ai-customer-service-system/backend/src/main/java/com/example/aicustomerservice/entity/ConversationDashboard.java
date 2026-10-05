package com.example.aicustomerservice.entity;

import java.time.Instant;
import java.util.Map;

/**
 * 会话运营摘要。所有数字由持久化会话计算，不依赖浏览器本地状态。
 */
public record ConversationDashboard(
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
    public ConversationDashboard {
        channelCounts = Map.copyOf(channelCounts);
        statusCounts = Map.copyOf(statusCounts);
    }
}
