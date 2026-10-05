package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.SupportDecision;
import java.time.Instant;
import java.util.List;

public record CustomerSupportResponse(
    String requestId,
    Instant receivedAt,
    long elapsedMs,
    String sessionId,
    String customerId,
    String intent,
    double intentConfidence,
    String answer,
    boolean handoffRequired,
    String ticketId,
    String ticketStatus,
    List<SourceResponse> sources,
    List<ToolTraceResponse> toolTraces,
    List<GuardrailResponse> guardrails,
    boolean cachedAnswer,
    String modelTier,
    boolean queued,
    int queueDepth,
    String modelRuntime,
    String severity,
    String suggestedNextAction
) {
    public static CustomerSupportResponse from(SupportDecision decision) {
        return from(decision, "", Instant.now(), 0);
    }

    public static CustomerSupportResponse from(
        SupportDecision decision,
        String requestId,
        Instant receivedAt,
        long elapsedMs
    ) {
        return new CustomerSupportResponse(
            requestId,
            receivedAt,
            elapsedMs,
            decision.sessionId(),
            decision.customerId(),
            decision.intent().type().name(),
            decision.intent().confidence(),
            decision.answer(),
            decision.handoffRequired(),
            decision.ticket() == null ? null : decision.ticket().ticketId(),
            decision.ticket() == null ? null : decision.ticket().status().name(),
            decision.sources().stream().map(SourceResponse::from).toList(),
            decision.toolTraces().stream().map(ToolTraceResponse::from).toList(),
            decision.guardrails().stream().map(GuardrailResponse::from).toList(),
            decision.cachedAnswer(),
            decision.modelTier().name(),
            decision.queued(),
            decision.queueDepth(),
            decision.modelRuntime(),
            severity(decision),
            suggestedNextAction(decision)
        );
    }

    private static String severity(SupportDecision decision) {
        if (decision.ticket() != null && "URGENT".equals(decision.ticket().priority().name())) {
            return "P1";
        }
        if (decision.handoffRequired()) {
            return "P2";
        }
        if (!decision.guardrails().isEmpty() || decision.queued()) {
            return "P3";
        }
        return "P4";
    }

    private static String suggestedNextAction(SupportDecision decision) {
        if (decision.ticket() != null) {
            return "进入工单列表分配处理人，并在时间线记录人工确认结果。";
        }
        if (decision.cachedAnswer()) {
            return "命中热点问题缓存，可以直接回复并持续观察知识版本。";
        }
        if (decision.queued()) {
            return "模型调用已进入队列，需要关注队列深度和接口耗时。";
        }
        return "AI 可直接回复，保留知识来源和工具轨迹用于复盘。";
    }
}
