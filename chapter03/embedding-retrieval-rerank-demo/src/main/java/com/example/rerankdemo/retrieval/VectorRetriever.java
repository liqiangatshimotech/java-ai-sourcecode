package com.example.rerankdemo.retrieval;

import com.example.rerankdemo.model.KnowledgeChunk;
import com.example.rerankdemo.model.SearchHit;

import java.util.Comparator;
import java.util.List;

/**
 * 一阶段向量召回。
 *
 * <p>它的目标不是直接给最终答案，而是用较低成本从大量 Chunk 中找出候选池。
 * 只拿 Top1 很危险，因为后面的重排没有机会修正一个没被召回的正确文档。
 */
public final class VectorRetriever {

    private final ConceptVectorizer vectorizer;
    private final List<KnowledgeChunk> chunks;

    public VectorRetriever(List<KnowledgeChunk> chunks, ConceptVectorizer vectorizer) {
        this.chunks = List.copyOf(chunks);
        this.vectorizer = vectorizer;
    }

    public List<SearchHit> search(String query, String tenantId, int topK) {
        double[] queryVector = vectorizer.vectorize(query);

        return chunks.stream()
            .filter(chunk -> chunk.tenantId().equals(tenantId))
            .map(chunk -> {
                double score = vectorizer.cosineSimilarity(queryVector, vectorizer.vectorize(chunk.searchableText()));
                return new SearchHit(chunk, score, "vector");
            })
            .sorted(Comparator.comparingDouble(SearchHit::score).reversed())
            .limit(Math.max(1, topK))
            .toList();
    }
}
