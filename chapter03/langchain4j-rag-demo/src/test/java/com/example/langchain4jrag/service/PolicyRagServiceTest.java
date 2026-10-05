package com.example.langchain4jrag.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyRagServiceTest {

    @Test
    void answersDuplicateChargeQuestionWithAcmeSourcesOnly() {
        PolicyRagService service = PolicyRagService.createDefault();

        PolicyRagService.AskResult result =
            service.ask("企业版客户重复扣费怎么办？", "acme", 4);

        assertTrue(result.answer().contains("重复扣费"));
        assertTrue(result.sources().stream()
            .anyMatch(source -> source.source().equals("acme-refund-policy.md")));
        assertTrue(result.sources().stream()
            .allMatch(source -> source.tenantId().equals("acme")));
    }

    @Test
    void tenantFilterKeepsBetaRefundPolicySeparate() {
        PolicyRagService service = PolicyRagService.createDefault();

        PolicyRagService.AskResult result =
            service.ask("客户重复扣费，退款多久到账？", "beta", 4);

        assertFalse(result.sources().isEmpty());
        assertTrue(result.sources().stream()
            .allMatch(source -> source.tenantId().equals("beta")));
        assertTrue(result.sources().stream()
            .anyMatch(source -> source.source().equals("beta-refund-policy.md")));
    }

    @Test
    void indexesAllKnowledgeDocuments() {
        PolicyRagService service = PolicyRagService.createDefault();

        assertEquals(4, service.indexedDocuments());
    }
}
