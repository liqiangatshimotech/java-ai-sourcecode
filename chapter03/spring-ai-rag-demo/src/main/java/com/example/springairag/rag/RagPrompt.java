package com.example.springairag.rag;

import java.util.List;

public record RagPrompt(
    String system,
    String user,
    String question,
    String tenantId,
    List<RetrievedChunk> retrievedChunks
) {
}
