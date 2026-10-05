package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.AiRunAudit;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 内存模式的 AI Run 审计，包含三条可直接展示的生产化种子记录。 */
public class InMemoryAiRunDao implements AiRunDao {

    private final Map<String, AiRunAudit> runs = new ConcurrentHashMap<>();

    public InMemoryAiRunDao(Clock clock) {
        this(clock, true);
    }

    public InMemoryAiRunDao(Clock clock, boolean seedData) {
        if (seedData) {
            seed(clock.instant());
        }
    }

    @Override
    public AiRunAudit save(AiRunAudit run) {
        runs.put(run.requestId(), run);
        return run;
    }

    @Override
    public Optional<AiRunAudit> findByRequestId(String requestId) {
        return Optional.ofNullable(runs.get(requestId));
    }

    @Override
    public List<AiRunAudit> search(String conversationId, Boolean handoff, int limit) {
        return runs.values().stream()
            .filter(run -> conversationId == null || conversationId.isBlank() || run.conversationId().equals(conversationId))
            .filter(run -> handoff == null || run.handoff() == handoff)
            .sorted(Comparator.comparing(AiRunAudit::createdAt).reversed())
            .limit(Math.max(1, Math.min(limit, 200)))
            .toList();
    }

    private void seed(Instant now) {
        save(new AiRunAudit(
            "REQ-SEED-REFUND", "CONV-20260710-001", "DEEPSEEK", "deepseek-v4-flash", "REFUND", 0.93,
            List.of("KB-REFUND-001@v2026.06 退款与售后规则 score=0.91"),
            List.of("order-api：订单 SO202606210004，状态 REFUNDING，金额 5600.00"),
            List.of("HALLUCINATION_RISK：高价值退款需要人工确认仓库拦截"),
            1380, true, now.minusSeconds(7 * 60)
        ));
        save(new AiRunAudit(
            "REQ-SEED-LOGISTICS", "CONV-20260710-002", "DEEPSEEK", "deepseek-v4-flash", "LOGISTICS_QUERY", 0.89,
            List.of("KB-SHIPPING-001@v2026.06 物流查询规则 score=0.87"),
            List.of("logistics-api：顺丰 SF202606210002，运输中"),
            List.of("KNOWLEDGE_VERSION_DRIFT：已记录引用版本"),
            920, false, now.minusSeconds(31 * 60)
        ));
        save(new AiRunAudit(
            "REQ-SEED-INVOICE", "CONV-20260710-003", "OLLAMA", "qwen2.5:7b", "INVOICE", 0.84,
            List.of("KB-INVOICE-001@v2026.06 发票处理规则 score=0.90"),
            List.of(),
            List.of("KNOWLEDGE_VERSION_DRIFT：已记录引用版本"),
            640, false, now.minusSeconds(68 * 60)
        ));
    }
}
