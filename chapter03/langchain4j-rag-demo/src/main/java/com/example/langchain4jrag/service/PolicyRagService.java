package com.example.langchain4jrag.service;

import com.example.langchain4jrag.assistant.PolicyAssistant;
import com.example.langchain4jrag.model.GroundedChatModel;
import com.example.langchain4jrag.model.HashingEmbeddingModel;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

public class PolicyRagService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatModel chatModel;
    private final int indexedDocuments;

    public PolicyRagService(
        EmbeddingModel embeddingModel,
        EmbeddingStore<TextSegment> embeddingStore,
        ChatModel chatModel,
        int indexedDocuments
    ) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chatModel = chatModel;
        this.indexedDocuments = indexedDocuments;
    }

    /** 测试/离线固定入口，不受运行机器是否配置 Key 影响。 */
    public static PolicyRagService createDefault() { return createWithModel(new GroundedChatModel()); }

    /** 只有显式开关才调用真实模型；检索仍用教学 Hashing 向量，不能冒充语义Embedding。 */
    public static PolicyRagService createFromEnvironment() {
        if (!Boolean.parseBoolean(System.getenv().getOrDefault("RUN_MODEL_CALL","false"))) return createDefault();
        return createWithModel(com.example.langchain4jrag.config.CourseChatModelFactory.create(System.getenv()));
    }

    public static PolicyRagService createWithModel(ChatModel model) {
        List<Document> documents = new KnowledgeBaseLoader().load();
        EmbeddingModel embeddingModel = new HashingEmbeddingModel();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        DocumentSplitter splitter = DocumentSplitters.recursive(650, 80);

        EmbeddingStoreIngestor.builder()
            .documentSplitter(splitter)
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .build()
            .ingest(documents);

        return new PolicyRagService(
            embeddingModel,
            embeddingStore,
            model,
            documents.size()
        );
    }

    public AskResult ask(String question, String tenantId, int topK) {
        PolicyAssistant assistant = assistantForTenant(tenantId, topK);
        Result<String> result = assistant.answer(question);

        return new AskResult(
            result.content(),
            result.sources().stream().map(this::toSource).toList()
        );
    }

    public int indexedDocuments() {
        return indexedDocuments;
    }

    private PolicyAssistant assistantForTenant(String tenantId, int topK) {
        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(Math.max(1, topK))
            .minScore(0.0)
            .filter(metadataKey("tenantId").isEqualTo(tenantId))
            .build();

        RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
            .contentRetriever(retriever)
            .contentInjector(DefaultContentInjector.builder()
                .metadataKeysToInclude(List.of("title", "source", "tenantId"))
                .build())
            .build();

        return AiServices.builder(PolicyAssistant.class)
            .chatModel(chatModel)
            .retrievalAugmentor(augmentor)
            .storeRetrievedContentInChatMemory(false)
            .build();
    }

    private RetrievedSource toSource(Content content) {
        TextSegment segment = content.textSegment();
        Object score = content.metadata().get(ContentMetadata.SCORE);
        return new RetrievedSource(
            segment.metadata().getString("tenantId"),
            segment.metadata().getString("title"),
            segment.metadata().getString("source"),
            score instanceof Number number ? number.doubleValue() : 0.0,
            segment.text()
        );
    }

    public record AskResult(String answer, List<RetrievedSource> sources) {
    }

    public record RetrievedSource(
        String tenantId,
        String title,
        String source,
        double score,
        String text
    ) {
    }
}
