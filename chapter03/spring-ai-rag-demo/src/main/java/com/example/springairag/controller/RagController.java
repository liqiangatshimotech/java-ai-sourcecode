package com.example.springairag.controller;

import com.example.springairag.dto.AskQuestionRequest;
import com.example.springairag.dto.AskQuestionResponse;
import com.example.springairag.rag.KnowledgeDocument;
import com.example.springairag.service.RagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public AskQuestionResponse ask(@Valid @RequestBody AskQuestionRequest request) {
        return ragService.ask(request);
    }

    @GetMapping("/documents")
    public Map<String, Object> documents() {
        List<KnowledgeDocument> documents = ragService.documents();
        return Map.of(
            "documentCount", documents.size(),
            "chunkCount", ragService.indexedChunks(),
            "documents", documents.stream()
                .map(document -> Map.of(
                    "id", document.id(),
                    "title", document.title(),
                    "tenantId", document.tenantId(),
                    "source", document.source(),
                    "tags", document.tags()
                ))
                .toList()
        );
    }
}
