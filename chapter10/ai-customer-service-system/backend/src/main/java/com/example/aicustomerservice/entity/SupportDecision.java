package com.example.aicustomerservice.entity;

import java.util.List;

public record SupportDecision(
    String sessionId,
    String customerId,
    SupportIntent intent,
    String answer,
    boolean handoffRequired,
    SupportTicket ticket,
    List<RetrievedKnowledge> sources,
    List<ToolTrace> toolTraces,
    List<ProductionGuardrail> guardrails,
    boolean cachedAnswer,
    ModelTier modelTier,
    boolean queued,
    int queueDepth,
    String modelRuntime,
    com.example.aicustomerservice.ai.ModelCompletion completion
) {
    public SupportDecision(String sessionId, String customerId, SupportIntent intent, String answer,
        boolean handoffRequired, SupportTicket ticket, List<RetrievedKnowledge> sources, List<ToolTrace> toolTraces,
        List<ProductionGuardrail> guardrails, boolean cachedAnswer, ModelTier modelTier, boolean queued,
        int queueDepth, String modelRuntime) {
        this(sessionId, customerId, intent, answer, handoffRequired, ticket, sources, toolTraces,
            guardrails, cachedAnswer, modelTier, queued, queueDepth, modelRuntime, null);
    }
}
