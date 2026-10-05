package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.RetrievedKnowledge;

public record SourceResponse(
    String documentId,
    String version,
    String title,
    String text,
    double score
) {
    public static SourceResponse from(RetrievedKnowledge knowledge) {
        return new SourceResponse(
            knowledge.documentId(),
            knowledge.version(),
            knowledge.title(),
            knowledge.text(),
            knowledge.score()
        );
    }
}
