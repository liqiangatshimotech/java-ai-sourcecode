package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.KnowledgeChunk;

public record KnowledgeChunkResponse(
    String chunkId,
    String documentId,
    String version,
    String title,
    String text
) {
    public static KnowledgeChunkResponse from(KnowledgeChunk chunk) {
        return new KnowledgeChunkResponse(
            chunk.chunkId(),
            chunk.documentId(),
            chunk.version(),
            chunk.title(),
            chunk.text()
        );
    }
}
