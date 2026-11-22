package com.example.aicustomerservice.service;

import com.example.aicustomerservice.dao.InMemoryKnowledgeDao;
import com.example.aicustomerservice.entity.KnowledgeDocument;
import java.time.Clock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalServiceTest {

    @Test
    void shouldRetrieveRefundPolicy() {
        RetrievalService retrievalService = new RetrievalService(
            new InMemoryKnowledgeDao(Clock.systemUTC()),
            new SemanticChunker()
        );

        var results = retrievalService.search("企业客户退款需要多久", 3);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).title()).contains("退款");
        assertThat(results.get(0).score()).isGreaterThan(0.3);
    }

    @Test
    void shouldRetrieveImportedKnowledgeOnlyAfterPublish() {
        Clock clock = Clock.systemUTC();
        InMemoryKnowledgeDao dao = new InMemoryKnowledgeDao(clock);
        SemanticChunker chunker = new SemanticChunker();
        KnowledgeLifecycleService lifecycleService = new KnowledgeLifecycleService(dao, chunker, clock);
        RetrievalService retrievalService = new RetrievalService(dao, chunker);

        // 新导入的内容先进入 DRAFT，不能因为保存成功就立即影响客服回答。
        KnowledgeDocument draft = lifecycleService.importDraft(
            "企业客户加急补发规则",
            "企业客户遇到货损时，可以申请加急补发，客服必须先核对订单和物流签收状态。",
            "v2026.06-hotfix",
            "FEISHU_DOC",
            "售后 SOP",
            "support-admin"
        );
        assertThat(retrievalService.search("企业客户加急补发", 3))
            .noneMatch(result -> result.documentId().equals(draft.documentId()));

        // 提交审核并发布后，正确文档应当进入召回结果。
        lifecycleService.submitReview(draft.documentId(), "support-admin");
        lifecycleService.publish(draft.documentId(), "support-lead");
        assertThat(retrievalService.search("企业客户加急补发", 3))
            .anyMatch(result -> result.documentId().equals(draft.documentId()));

        // 下线后必须立即退出召回，避免旧政策继续影响回答。
        lifecycleService.disable(draft.documentId(), "support-lead");
        assertThat(retrievalService.search("企业客户加急补发", 3))
            .noneMatch(result -> result.documentId().equals(draft.documentId()));
    }
}
