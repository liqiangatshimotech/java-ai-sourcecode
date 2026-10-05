package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.KnowledgeDocument;
import java.time.Instant;

public record KnowledgeDocumentResponse(
    String documentId,
    String version,
    String title,
    String content,
    String status,
    String sourceType,
    String sourceName,
    String operator,
    Instant createdAt,
    Instant updatedAt
) {
    public static KnowledgeDocumentResponse from(KnowledgeDocument document) {
        return new KnowledgeDocumentResponse(
            document.documentId(),
            document.version(),
            document.title(),
            document.content(),
            document.status().name(),
            document.sourceType(),
            document.sourceName(),
            document.operator(),
            document.createdAt(),
            document.updatedAt()
        );
    }
}
