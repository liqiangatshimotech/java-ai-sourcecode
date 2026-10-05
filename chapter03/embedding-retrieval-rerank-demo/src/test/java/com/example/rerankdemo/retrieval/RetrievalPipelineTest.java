package com.example.rerankdemo.retrieval;

import com.example.rerankdemo.model.DemoCorpus;
import com.example.rerankdemo.model.SearchHit;
import com.example.rerankdemo.rerank.BusinessReranker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetrievalPipelineTest {

    private static final String QUERY = "报销凭证公司名称不对，能不能重新开？";

    @Test
    void weakVectorTop1CanChooseADistractor() {
        VectorRetriever retriever = new VectorRetriever(DemoCorpus.supportChunks(), new ConceptVectorizer());

        SearchHit top1 = retriever.search(QUERY, "acme", 1).getFirst();

        assertEquals("ACME-COMPANY-001", top1.chunk().id());
        assertNotEquals("ACME-INVOICE-001", top1.chunk().id());
    }

    @Test
    void rerankingOnlyWorksWhenTheAnswerIsAlreadyInCandidatePool() {
        VectorRetriever retriever = new VectorRetriever(DemoCorpus.supportChunks(), new ConceptVectorizer());
        BusinessReranker reranker = new BusinessReranker();

        SearchHit rerankedFromTop1 = reranker.rerank(QUERY, retriever.search(QUERY, "acme", 1), 1).getFirst();
        SearchHit rerankedFromTop3 = reranker.rerank(QUERY, retriever.search(QUERY, "acme", 3), 1).getFirst();

        assertEquals("ACME-COMPANY-001", rerankedFromTop1.chunk().id());
        assertEquals("ACME-INVOICE-001", rerankedFromTop3.chunk().id());
    }

    @Test
    void queryExpansionAndHybridRetrievalKeepTheRightDocumentVisible() {
        VectorRetriever vectorRetriever = new VectorRetriever(DemoCorpus.supportChunks(), new ConceptVectorizer());
        KeywordRetriever keywordRetriever = new KeywordRetriever(DemoCorpus.supportChunks());
        HybridRetriever hybridRetriever = new HybridRetriever(vectorRetriever, keywordRetriever);
        QueryExpander expander = new QueryExpander();

        List<SearchHit> hits = hybridRetriever.search(expander.expand(QUERY), "acme", 3);

        assertTrue(hits.stream().anyMatch(hit -> hit.chunk().id().equals("ACME-INVOICE-001")));
    }

    @Test
    void tenantFilterRemovesOtherTenantDocumentsBeforeReranking() {
        VectorRetriever retriever = new VectorRetriever(DemoCorpus.supportChunks(), new ConceptVectorizer());

        List<SearchHit> hits = retriever.search("发票重开", "acme", 10);

        assertTrue(hits.stream().noneMatch(hit -> hit.chunk().id().equals("BETA-INVOICE-001")));
    }
}
