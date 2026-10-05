package com.example.springairag.dto;

public record RetrievedSource(
    String chunkId,
    String documentId,
    String title,
    String source,
    double score
) {
}
