package com.example.aicustomerservice.service;

import com.example.aicustomerservice.ai.ModelSettings;
import com.example.aicustomerservice.common.BusinessException;
import com.example.aicustomerservice.dao.AiRunDao;
import com.example.aicustomerservice.entity.AiRunAudit;
import com.example.aicustomerservice.entity.SupportDecision;
import java.time.Instant;
import java.util.List;

/** AI 编排审计服务，把运行中的复杂对象转换成可安全展示、可长期保存的摘要。 */
public class AiRunService {

    private final AiRunDao aiRunDao;

    public AiRunService(AiRunDao aiRunDao) {
        this.aiRunDao = aiRunDao;
    }

    public AiRunAudit record(
        String requestId,
        String conversationId,
        ModelSettings settings,
        SupportDecision decision,
        long elapsedMs,
        Instant createdAt
    ) {
        AiRunAudit run = new AiRunAudit(
            requestId,
            conversationId,
            settings.provider().name(),
            decision.completion() != null && decision.completion().actualModel() != null
                ? decision.completion().actualModel() : settings.modelName(),
            decision.intent().type().name(),
            decision.intent().confidence(),
            decision.sources().stream()
                .map(source -> "%s@%s %s score=%.3f".formatted(
                    source.documentId(), source.version(), source.title(), source.score()
                ))
                .toList(),
            decision.toolTraces().stream()
                .map(tool -> tool.toolName() + "：" + tool.summary())
                .toList(),
            decision.guardrails().stream()
                .map(guardrail -> guardrail.problem().name() + "：" + guardrail.action())
                .toList(),
            elapsedMs,
            decision.handoffRequired(),
            createdAt,
            decision.cachedAnswer() ? "CACHED" : "SUCCEEDED", null,
            com.example.aicustomerservice.ai.SupportPrompt.VERSION,
            decision.cachedAnswer() ? Long.valueOf(0) : decision.completion() == null ? null : decision.completion().inputTokens(),
            decision.cachedAnswer() ? Long.valueOf(0) : decision.completion() == null ? null : decision.completion().outputTokens()
        );
        return aiRunDao.save(run);
    }

    /** 业务动作前先保存 RUNNING；数据库写失败时停止请求，不出现无审计的副作用。 */
    public AiRunAudit begin(String requestId, String conversationId, ModelSettings settings, Instant at) {
        return lifecycle(requestId, conversationId, settings, at, "RUNNING", null, 0);
    }

    public AiRunAudit failed(String requestId, String conversationId, ModelSettings settings,
                             Instant at, RuntimeException failure, long elapsedMs) {
        String code = failure instanceof BusinessException business ? business.code() : "INTERNAL_ERROR";
        // 不存供应商响应体或异常完整文本，避免把令牌、客户原文带进审计。
        return lifecycle(requestId, conversationId, settings, at,
            "MODEL_BUSY".equals(code) ? "REJECTED" : "FAILED", code, elapsedMs);
    }

    private AiRunAudit lifecycle(String id, String conversation, ModelSettings settings, Instant at,
                                 String state, String code, long elapsed) {
        return aiRunDao.save(new AiRunAudit(id, conversation, settings.provider().name(), settings.modelName(),
            "UNKNOWN", 0, List.of(), List.of(), List.of(), elapsed, false, at, state, code,
            com.example.aicustomerservice.ai.SupportPrompt.VERSION, null, null));
    }

    public List<AiRunAudit> search(String conversationId, Boolean handoff, int limit) {
        return aiRunDao.search(conversationId, handoff, Math.max(1, Math.min(limit, 200)));
    }

    public AiRunAudit get(String requestId) {
        return aiRunDao.findByRequestId(requestId)
            .orElseThrow(() -> new BusinessException("AI_RUN_NOT_FOUND", "AI 运行记录不存在：" + requestId));
    }
}
