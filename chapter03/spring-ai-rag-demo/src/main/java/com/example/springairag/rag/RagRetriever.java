package com.example.springairag.rag;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RagRetriever {

    private static final Logger log = LoggerFactory.getLogger(RagRetriever.class);

    private final MarkdownKnowledgeDocumentLoader loader;
    private final SimpleTextSplitter splitter;
    private final HashingEmbeddingModel embeddingModel;
    private final InMemoryVectorStore vectorStore;
    private List<KnowledgeDocument> documents = List.of();

    public RagRetriever(
        MarkdownKnowledgeDocumentLoader loader,
        SimpleTextSplitter splitter,
        HashingEmbeddingModel embeddingModel,
        InMemoryVectorStore vectorStore
    ) {
        this.loader = loader;
        this.splitter = splitter;
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void index() {
        documents = loader.loadDocuments();
        List<DocumentChunk> allChunks = new ArrayList<>();

        for (KnowledgeDocument document : documents) {
            List<DocumentChunk> chunks = splitter.split(document);
            allChunks.addAll(chunks);
            for (DocumentChunk chunk : chunks) {
                vectorStore.add(chunk, embeddingModel.embed(textForEmbedding(chunk)));
            }
        }

        log.info(
            "RAG knowledge base indexed documents={} chunks={}",
            documents.size(),
            allChunks.size()
        );
    }

    public List<RetrievedChunk> retrieve(String question, String tenantId, int topK) {
        return vectorStore.search(embeddingModel.embed(question), tenantId, topK);
    }

    public List<KnowledgeDocument> documents() {
        return documents;
    }

    public int indexedChunks() {
        return vectorStore.size();
    }

    private String textForEmbedding(DocumentChunk chunk) {
        return chunk.title() + "\n" + String.join(",", chunk.tags()) + "\n" + chunk.content();
    }
}
