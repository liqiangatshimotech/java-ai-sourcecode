package com.example.aicustomerservice.entity;

import java.time.Instant;

public record KnowledgeDocument(
    String documentId,
    String version,
    String title,
    String content,
    KnowledgeStatus status,
    String sourceType,
    String sourceName,
    String operator,
    Instant createdAt,
    Instant updatedAt
) {
    public boolean published() {
        return status == KnowledgeStatus.PUBLISHED;
    }

    public KnowledgeDocument withStatus(KnowledgeStatus newStatus, String operator, Instant now) {
        return new KnowledgeDocument(
            documentId,
            version,
            title,
            content,
            newStatus,
            sourceType,
            sourceName,
            operator,
            createdAt,
            now
        );
    }
}
