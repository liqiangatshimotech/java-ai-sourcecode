package com.example.springairag.rag;

public record RetrievedChunk(
    DocumentChunk chunk,
    double score
) {
}
