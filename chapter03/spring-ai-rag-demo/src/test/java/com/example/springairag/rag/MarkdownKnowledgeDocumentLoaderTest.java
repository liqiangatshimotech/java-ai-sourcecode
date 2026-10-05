package com.example.springairag.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownKnowledgeDocumentLoaderTest {

    @Test
    void loadsMarkdownDocumentsWithMetadata() {
        MarkdownKnowledgeDocumentLoader loader = new MarkdownKnowledgeDocumentLoader();

        List<KnowledgeDocument> documents = loader.loadDocuments();

        assertThat(documents)
            .extracting(KnowledgeDocument::id)
            .contains("acme-refund-policy", "acme-invoice-policy", "acme-shipping-policy", "beta-refund-policy");

        KnowledgeDocument refundPolicy = documents.stream()
            .filter(document -> document.id().equals("acme-refund-policy"))
            .findFirst()
            .orElseThrow();

        assertThat(refundPolicy.title()).isEqualTo("ACME 企业版退款与重复扣费处理规则");
        assertThat(refundPolicy.tenantId()).isEqualTo("acme");
        assertThat(refundPolicy.tags()).contains("billing", "refund");
        assertThat(refundPolicy.content()).contains("重复扣费");
    }
}
