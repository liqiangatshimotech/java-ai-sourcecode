package com.example.aicustomerservice.entity;

public record RetrievedKnowledge(
    String chunkId,
    String documentId,
    String version,
    String title,
    String text,
    double score
) {
}
