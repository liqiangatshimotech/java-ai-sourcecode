package com.example.aicustomerservice.entity;

public record KnowledgeChunk(
    String chunkId,
    String documentId,
    String version,
    String title,
    String text
) {
}
