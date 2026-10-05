package com.example.springairag.rag;

import com.example.springairag.config.RagProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class InMemoryVectorStore {

    private final RagProperties properties;
    private final List<Entry> entries = new ArrayList<>();

    public InMemoryVectorStore(RagProperties properties) {
        this.properties = properties;
    }

    public void add(DocumentChunk chunk, double[] embedding) {
        entries.add(new Entry(chunk, embedding));
    }

    public List<RetrievedChunk> search(double[] queryEmbedding, String tenantId, int topK) {
        int limit = topK <= 0 ? properties.getTopK() : topK;
        double threshold = properties.getSimilarityThreshold();

        return entries.stream()
            .filter(entry -> tenantId == null || tenantId.isBlank() || tenantId.equals(entry.chunk().tenantId()))
            .map(entry -> new RetrievedChunk(entry.chunk(), cosine(queryEmbedding, entry.embedding())))
            .filter(result -> result.score() >= threshold)
            .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
            .limit(limit)
            .toList();
    }

    public int size() {
        return entries.size();
    }

    private double cosine(double[] left, double[] right) {
        double score = 0.0;
        int max = Math.min(left.length, right.length);
        for (int i = 0; i < max; i++) {
            score += left[i] * right[i];
        }
        return score;
    }

    private record Entry(DocumentChunk chunk, double[] embedding) {
    }
}
