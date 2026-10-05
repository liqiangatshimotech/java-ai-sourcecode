package com.example.embeddingdemo.search;

import com.example.embeddingdemo.model.KnowledgeEntry;
import com.example.embeddingdemo.model.SearchResult;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 基于 Embedding 的语义检索。
 *
 * <p>索引阶段：把知识库文本变成向量，和原文一起存起来。
 * 查询阶段：把问题也变成向量，再和每条知识的向量算相似度。
 */
public final class EmbeddingSearchEngine {

    private final EmbeddingModel embeddingModel;
    private final List<IndexedEntry> index;

    private EmbeddingSearchEngine(EmbeddingModel embeddingModel, List<IndexedEntry> index) {
        this.embeddingModel = embeddingModel;
        this.index = List.copyOf(index);
    }

    public static EmbeddingSearchEngine index(List<KnowledgeEntry> entries, EmbeddingModel embeddingModel) {
        List<TextSegment> segments = entries.stream()
            .map(entry -> TextSegment.from(entry.searchableText()))
            .toList();

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        if (embeddings.size() != entries.size()) {
            throw new IllegalStateException("Embedding 返回数量和知识条目数量不一致");
        }

        List<IndexedEntry> index = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            index.add(new IndexedEntry(entries.get(i), embeddings.get(i).vector()));
        }
        return new EmbeddingSearchEngine(embeddingModel, index);
    }

    public List<SearchResult> search(String query, int topK) {
        float[] queryVector = embeddingModel.embed(query).content().vector();

        return index.stream()
            .map(indexed -> new SearchResult(
                indexed.entry(),
                VectorMath.cosineSimilarity(queryVector, indexed.vector())
            ))
            .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
            .limit(Math.max(1, topK))
            .toList();
    }

    private record IndexedEntry(KnowledgeEntry entry, float[] vector) {
    }
}
