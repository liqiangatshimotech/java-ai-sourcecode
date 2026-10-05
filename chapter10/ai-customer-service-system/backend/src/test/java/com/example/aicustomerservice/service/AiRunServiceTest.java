package com.example.aicustomerservice.service;

import com.example.aicustomerservice.ai.AiProvider;
import com.example.aicustomerservice.ai.ModelSettings;
import com.example.aicustomerservice.common.BusinessException;
import com.example.aicustomerservice.dao.InMemoryAiRunDao;
import com.example.aicustomerservice.entity.EnterpriseProblemType;
import com.example.aicustomerservice.entity.IntentType;
import com.example.aicustomerservice.entity.ModelTier;
import com.example.aicustomerservice.entity.ProductionGuardrail;
import com.example.aicustomerservice.entity.RetrievedKnowledge;
import com.example.aicustomerservice.entity.SupportDecision;
import com.example.aicustomerservice.entity.SupportIntent;
import com.example.aicustomerservice.entity.ToolTrace;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiRunServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");

    @Test
    void shouldPersistSafeTraceSummariesAndSupportFiltering() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        AiRunService service = new AiRunService(new InMemoryAiRunDao(clock, false));
        ModelSettings settings = new ModelSettings(
            AiProvider.DEEPSEEK,
            "https://api.deepseek.com/v1",
            "secret-must-not-be-stored",
            "deepseek-v4-flash",
            0.2
        );
        SupportDecision decision = new SupportDecision(
            "CONV-TEST-001",
            "C10001",
            new SupportIntent(IntentType.REFUND, 0.92, "命中退款意图"),
            "需要人工确认。",
            true,
            null,
            List.of(new RetrievedKnowledge(
                "CHUNK-1", "KB-REFUND-001", "v1", "退款规则", "证据正文", 0.88
            )),
            List.of(new ToolTrace("order-api", "订单金额 5600 元")),
            List.of(new ProductionGuardrail(
                EnterpriseProblemType.HALLUCINATION_RISK,
                "需要仓库确认",
                "禁止自动承诺退款",
                true
            )),
            false,
            ModelTier.ESCALATED,
            false,
            0,
            "DEEPSEEK / deepseek-v4-flash"
        );

        var saved = service.record("REQ-TEST-001", "CONV-TEST-001", settings, decision, 321, NOW);

        assertThat(saved.provider()).isEqualTo("DEEPSEEK");
        assertThat(saved.model()).isEqualTo("deepseek-v4-flash");
        assertThat(saved.sourceSummaries()).singleElement().asString().contains("KB-REFUND-001@v1");
        assertThat(saved.toolSummaries()).containsExactly("order-api：订单金额 5600 元");
        assertThat(saved.guardrailSummaries()).containsExactly("HALLUCINATION_RISK：禁止自动承诺退款");
        assertThat(saved.toString()).doesNotContain("secret-must-not-be-stored");
        assertThat(service.search("CONV-TEST-001", true, 20)).containsExactly(saved);
        assertThat(service.get("REQ-TEST-001")).isEqualTo(saved);
    }

    @Test
    void shouldReturnBusinessErrorForMissingRun() {
        AiRunService service = new AiRunService(new InMemoryAiRunDao(
            Clock.fixed(NOW, ZoneOffset.UTC),
            false
        ));

        assertThatThrownBy(() -> service.get("REQ-NOT-FOUND"))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.code()).isEqualTo("AI_RUN_NOT_FOUND")
            );
    }
}
