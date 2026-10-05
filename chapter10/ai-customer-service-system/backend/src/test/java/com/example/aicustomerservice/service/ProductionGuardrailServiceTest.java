package com.example.aicustomerservice.service;

import com.example.aicustomerservice.entity.EnterpriseProblemType;
import com.example.aicustomerservice.entity.IntentType;
import com.example.aicustomerservice.entity.ProductionGuardrail;
import com.example.aicustomerservice.entity.RetrievedKnowledge;
import com.example.aicustomerservice.entity.SupportIntent;
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
}
