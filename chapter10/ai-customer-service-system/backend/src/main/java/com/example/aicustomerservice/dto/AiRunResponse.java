package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.AiRunAudit;
import java.time.Instant;
import java.util.List;

/** AI Run 列表和详情共用的只读响应。 */
public record AiRunResponse(
    String requestId,
    String conversationId,
    String provider,
    String model,
    String intent,
    double confidence,
    List<String> sourceSummaries,
    List<String> toolSummaries,
    List<String> guardrailSummaries,
    long elapsedMs,
    boolean handoff,
    Instant createdAt,
    String status, String errorCode, String promptVersion, Long inputTokens, Long outputTokens
) {
    public static AiRunResponse from(AiRunAudit run) {
        return new AiRunResponse(
            run.requestId(), run.conversationId(), run.provider(), run.model(), run.intent(), run.confidence(),
            run.sourceSummaries(), run.toolSummaries(), run.guardrailSummaries(), run.elapsedMs(),
            run.handoff(), run.createdAt(), run.status(), run.errorCode(), run.promptVersion(), run.inputTokens(), run.outputTokens()
        );
    }
}
