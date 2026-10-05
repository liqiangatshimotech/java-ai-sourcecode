package com.example.springairag.service;

import com.example.springairag.config.RagProperties;
import com.example.springairag.dto.AskQuestionRequest;
import com.example.springairag.dto.AskQuestionResponse;
import com.example.springairag.dto.RetrievedSource;
import com.example.springairag.rag.KnowledgeDocument;
import com.example.springairag.rag.RagPrompt;
import com.example.springairag.rag.RagPromptBuilder;
import com.example.springairag.rag.RagRetriever;
import com.example.springairag.rag.RetrievedChunk;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final RagRetriever retriever;
    private final RagPromptBuilder promptBuilder;
    private final AiGateway aiGateway;
    private final RagProperties properties;

    public RagService(
        RagRetriever retriever,
        RagPromptBuilder promptBuilder,
        AiGateway aiGateway,
        RagProperties properties
    ) {
        this.retriever = retriever;
        this.promptBuilder = promptBuilder;
        this.aiGateway = aiGateway;
        this.properties = properties;
    }

    public AskQuestionResponse ask(AskQuestionRequest request) {
        String tenantId = normalizeTenant(request.tenantId());
        int topK = request.topK() == null ? properties.getTopK() : request.topK();

        List<RetrievedChunk> chunks = retriever.retrieve(request.question(), tenantId, topK);
        RagPrompt prompt = promptBuilder.build(request.question(), tenantId, chunks);
        String answer = aiGateway.answer(prompt);

        return new AskQuestionResponse(
            answer,
            chunks.stream().map(this::toSource).toList(),
            chunks.stream().map(result -> result.chunk().content()).toList()
        );
    }

    public List<KnowledgeDocument> documents() {
        return retriever.documents();
    }

    public int indexedChunks() {
        return retriever.indexedChunks();
    }

    private String normalizeTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return properties.getDefaultTenantId();
        }
        return tenantId.trim();
    }

    private RetrievedSource toSource(RetrievedChunk retrieved) {
        return new RetrievedSource(
            retrieved.chunk().id(),
            retrieved.chunk().documentId(),
            retrieved.chunk().title(),
            retrieved.chunk().source(),
            Math.round(retrieved.score() * 10000.0) / 10000.0
        );
    }
}
