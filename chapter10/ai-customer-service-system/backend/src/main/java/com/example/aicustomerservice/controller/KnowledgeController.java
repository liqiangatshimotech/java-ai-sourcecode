package com.example.aicustomerservice.controller;

import com.example.aicustomerservice.dto.KnowledgeActionRequest;
import com.example.aicustomerservice.dto.KnowledgeChunkResponse;
import com.example.aicustomerservice.dto.KnowledgeDocumentResponse;
import com.example.aicustomerservice.dto.KnowledgeImportRequest;
import com.example.aicustomerservice.dto.KnowledgeSearchResponse;
import com.example.aicustomerservice.dto.SourceResponse;
import com.example.aicustomerservice.entity.KnowledgeStatus;
import com.example.aicustomerservice.service.KnowledgeLifecycleService;
import com.example.aicustomerservice.service.RetrievalService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/knowledge")
public class KnowledgeController {

    private final KnowledgeLifecycleService lifecycleService;
    private final RetrievalService retrievalService;

    public KnowledgeController(KnowledgeLifecycleService lifecycleService, RetrievalService retrievalService) {
        this.lifecycleService = lifecycleService;
        this.retrievalService = retrievalService;
    }

    @PostMapping("/documents")
    public KnowledgeDocumentResponse importDocument(@Valid @RequestBody KnowledgeImportRequest request) {
        return KnowledgeDocumentResponse.from(lifecycleService.importDraft(
            request.title(),
            request.content(),
            request.version(),
            request.sourceType(),
            request.sourceName(),
            com.example.aicustomerservice.security.SupportIdentity.operator(request.operator())
        ));
    }

    @GetMapping("/documents")
    public List<KnowledgeDocumentResponse> listDocuments(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) KnowledgeStatus status
    ) {
        return lifecycleService.search(keyword, status)
            .stream()
            .map(KnowledgeDocumentResponse::from)
            .toList();
    }

    @GetMapping("/documents/{documentId}")
    public KnowledgeDocumentResponse getDocument(@PathVariable String documentId) {
        return KnowledgeDocumentResponse.from(lifecycleService.get(documentId));
    }

    @GetMapping("/documents/{documentId}/chunks")
    public List<KnowledgeChunkResponse> previewChunks(@PathVariable String documentId) {
        return lifecycleService.previewChunks(documentId)
            .stream()
            .map(KnowledgeChunkResponse::from)
            .toList();
    }

    @PostMapping("/documents/{documentId}/submit-review")
    public KnowledgeDocumentResponse submitReview(
        @PathVariable String documentId,
        @Valid @RequestBody(required = false) KnowledgeActionRequest request
    ) {
        return KnowledgeDocumentResponse.from(lifecycleService.submitReview(documentId, operator(request)));
    }

    @PostMapping("/documents/{documentId}/publish")
    public KnowledgeDocumentResponse publish(
        @PathVariable String documentId,
        @Valid @RequestBody(required = false) KnowledgeActionRequest request
    ) {
        return KnowledgeDocumentResponse.from(lifecycleService.publish(documentId, operator(request)));
    }

    @PostMapping("/documents/{documentId}/disable")
    public KnowledgeDocumentResponse disable(
        @PathVariable String documentId,
        @Valid @RequestBody(required = false) KnowledgeActionRequest request
    ) {
        return KnowledgeDocumentResponse.from(lifecycleService.disable(documentId, operator(request)));
    }

    @GetMapping("/search")
    public KnowledgeSearchResponse search(
        @RequestParam String query,
        @RequestParam(defaultValue = "3") int limit
    ) {
        List<SourceResponse> results = retrievalService.search(query, limit)
            .stream()
            .map(SourceResponse::from)
            .toList();
        return new KnowledgeSearchResponse(query, results);
    }

    private String operator(KnowledgeActionRequest request) {
        return com.example.aicustomerservice.security.SupportIdentity.operator(request == null ? "online-operator" : request.operator());
    }
}
