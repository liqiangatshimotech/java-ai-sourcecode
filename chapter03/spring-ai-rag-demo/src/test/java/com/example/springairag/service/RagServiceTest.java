package com.example.springairag.service;

import com.example.springairag.config.RagProperties;
import com.example.springairag.dto.AskQuestionRequest;
import com.example.springairag.dto.AskQuestionResponse;
import com.example.springairag.rag.HashingEmbeddingModel;
import com.example.springairag.rag.InMemoryVectorStore;
import com.example.springairag.rag.MarkdownKnowledgeDocumentLoader;
import com.example.springairag.rag.RagPromptBuilder;
import com.example.springairag.rag.RagRetriever;
import com.example.springairag.rag.SimpleTextSplitter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RagServiceTest {

    @Test
    void answersRefundQuestionWithAcmeSources() {
        RagService service = createService(0.08);

        AskQuestionResponse response = service.ask(new AskQuestionRequest(
            "企业版客户重复扣费，还开不了发票，客服应该怎么处理？",
            "acme",
            4
        ));

        assertThat(response.sources()).isNotEmpty();
        assertThat(response.sources())
            .extracting(source -> source.documentId())
            .contains("acme-refund-policy");
        assertThat(response.sources())
            .extracting(source -> source.documentId())
            .doesNotContain("beta-refund-policy");
        assertThat(response.answer())
            .contains("Fake 模型演示回答")
            .contains("重复扣费");
    }

    @Test
    void filtersDocumentsByTenantMetadata() {
        RagService service = createService(0.08);

        AskQuestionResponse response = service.ask(new AskQuestionRequest(
            "退款申请要怎么处理？",
            "beta",
            4
        ));

        assertThat(response.sources())
            .extracting(source -> source.documentId())
            .contains("beta-refund-policy")
            .doesNotContain("acme-refund-policy");
    }

    @Test
    void refusesWhenNoContextIsRetrieved() {
        RagService service = createService(0.99);

        AskQuestionResponse response = service.ask(new AskQuestionRequest(
            "公司食堂午餐菜单是什么？",
            "acme",
            4
        ));

        assertThat(response.sources()).isEmpty();
        assertThat(response.answer()).contains("知识库中没有找到足够依据");
    }

    private RagService createService(double threshold) {
        RagProperties properties = new RagProperties();
        properties.setSimilarityThreshold(threshold);
        properties.setTopK(4);
        properties.setMaxChunkChars(650);
        properties.setChunkOverlapChars(80);
        properties.setEmbeddingDimensions(384);
        properties.setDefaultTenantId("acme");

        MarkdownKnowledgeDocumentLoader loader = new MarkdownKnowledgeDocumentLoader();
        SimpleTextSplitter splitter = new SimpleTextSplitter(properties);
        HashingEmbeddingModel embeddingModel = new HashingEmbeddingModel(properties);
        InMemoryVectorStore vectorStore = new InMemoryVectorStore(properties);
        RagRetriever retriever = new RagRetriever(loader, splitter, embeddingModel, vectorStore);
        retriever.index();

        return new RagService(
            retriever,
            new RagPromptBuilder(),
            new FakeGroundedAiGateway(),
            properties
        );
    }
}
