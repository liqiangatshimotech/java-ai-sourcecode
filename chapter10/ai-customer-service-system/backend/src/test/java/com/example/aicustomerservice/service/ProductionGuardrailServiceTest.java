package com.example.aicustomerservice.service;

import com.example.aicustomerservice.entity.EnterpriseProblemType;
import com.example.aicustomerservice.entity.IntentType;
import com.example.aicustomerservice.entity.ProductionGuardrail;
import com.example.aicustomerservice.entity.RetrievedKnowledge;
import com.example.aicustomerservice.entity.SupportIntent;
import com.example.aicustomerservice.entity.ToolTrace;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionGuardrailServiceTest {

    @Test
    void shouldMapLowRecallAndVersionTraceToGuardrails() {
        ProductionGuardrailService service = new ProductionGuardrailService();

        List<ProductionGuardrail> guardrails = service.assess(
            new SupportIntent(IntentType.FAQ, 0.7, "faq"),
            List.of(new RetrievedKnowledge(
                "chunk-1",
                "KB-REFUND-001",
                "v2026.06",
                "退款规则",
                "退款说明",
                0.2
            )),
            List.of(),
            null,
            List.of()
        );

        assertThat(guardrails)
            .anyMatch(guardrail -> guardrail.problem() == EnterpriseProblemType.KNOWLEDGE_RECALL_LOW
                && guardrail.requiresHandoff());
        assertThat(guardrails)
            .anyMatch(guardrail -> guardrail.problem() == EnterpriseProblemType.KNOWLEDGE_VERSION_DRIFT
                && guardrail.signal().contains("v2026.06"));
    }

    @Test
    void refundHandoffShouldFollowStructuredFlagInsteadOfSummaryWording() {
        ProductionGuardrailService service = new ProductionGuardrailService();
        SupportIntent refund = new SupportIntent(IntentType.REFUND, 0.86, "refund");
        List<RetrievedKnowledge> sources = List.of(new RetrievedKnowledge(
            "chunk-1", "KB-REFUND-001", "v2026.06", "退款与售后规则", "退款说明", 0.8
        ));

        // 摘要里没有“人工确认”四个字，但工具给出了需要人工的结构化结论：必须转人工。
        List<ProductionGuardrail> flagged = service.assess(
            refund,
            sources,
            List.of(new ToolTrace("refund-policy-engine", "当前订单状态暂不支持自动退款。创建工单给售后团队处理。", true)),
            "SO202606210004",
            List.of()
        );
        assertThat(flagged).anyMatch(guardrail -> guardrail.requiresHandoff()
            && guardrail.signal().equals("退款工具返回人工确认"));

        // 反过来，摘要里出现“人工确认”字样但结构化结论为 false：不能因为措辞就转人工。
        List<ProductionGuardrail> wordingOnly = service.assess(
            refund,
            sources,
            List.of(new ToolTrace("refund-policy-engine", "订单已签收，无需人工确认，可以引导用户提交退款申请。", false)),
            "SO202606210003",
            List.of()
        );
        assertThat(wordingOnly).noneMatch(ProductionGuardrail::requiresHandoff);
    }
}
