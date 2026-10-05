package com.example.aicustomerservice.entity;

import java.time.Instant;
import java.util.List;

/**
 * 一次 AI 客服编排的审计快照。
 *
 * 这里只保存可以安全展示的摘要，不保存 API Key 和完整系统 Prompt。requestId 与接口响应中的请求号一致，
 * 出现错误回复时可以从页面一路追溯到模型、知识版本、工具结果和护栏判断。
 */
public record AiRunAudit(
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
    String status,
    String errorCode,
    String promptVersion,
    Long inputTokens,
    Long outputTokens
) {
    /** 兼容旧种子数据；未计量的 Token 必须保持未知。 */
    public AiRunAudit(String requestId, String conversationId, String provider, String model, String intent,
        double confidence, List<String> sources, List<String> tools, List<String> guardrails,
        long elapsedMs, boolean handoff, Instant createdAt) {
        this(requestId, conversationId, provider, model, intent, confidence, sources, tools, guardrails,
            elapsedMs, handoff, createdAt, "SUCCEEDED", null, "legacy", null, null);
    }
    public AiRunAudit {
        sourceSummaries = List.copyOf(sourceSummaries);
        toolSummaries = List.copyOf(toolSummaries);
        guardrailSummaries = List.copyOf(guardrailSummaries);
    }
}
