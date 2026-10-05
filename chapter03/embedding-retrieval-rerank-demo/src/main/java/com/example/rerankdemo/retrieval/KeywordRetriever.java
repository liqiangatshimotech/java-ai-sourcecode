package com.example.rerankdemo.retrieval;

import com.example.rerankdemo.model.KnowledgeChunk;
import com.example.rerankdemo.model.SearchHit;

import java.util.Comparator;
import java.util.List;

/**
 * 关键词召回。
 *
 * <p>Embedding 擅长语义，关键词擅长精确匹配。混合召回经常比单一路径稳定：
 * 型号、订单号、错误码、产品名这类信息，关键词检索通常更可靠。
 */
public final class KeywordRetriever {

    private final List<KnowledgeChunk> chunks;

    public KeywordRetriever(List<KnowledgeChunk> chunks) {
        this.chunks = List.copyOf(chunks);
    }

    public List<SearchHit> search(String query, String tenantId, int topK) {
        List<String> terms = TextSignals.roughTerms(query);

        return chunks.stream()
            .filter(chunk -> chunk.tenantId().equals(tenantId))
            .map(chunk -> new SearchHit(chunk, keywordScore(chunk.searchableText(), terms), "keyword"))
            .sorted(Comparator.comparingDouble(SearchHit::score).reversed())
            .limit(Math.max(1, topK))
            .toList();
    }

    private double keywordScore(String text, List<String> terms) {
        if (terms.isEmpty()) {
            return 0.0;
        }

        String normalized = TextSignals.normalize(text);
        long matched = terms.stream()
            .filter(normalized::contains)
            .count();
        return (double) matched / terms.size();
    }
}
