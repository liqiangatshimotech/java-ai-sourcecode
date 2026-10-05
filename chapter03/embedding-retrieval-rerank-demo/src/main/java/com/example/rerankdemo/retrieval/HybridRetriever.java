package com.example.rerankdemo.retrieval;

import com.example.rerankdemo.model.KnowledgeChunk;
import com.example.rerankdemo.model.SearchHit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 混合召回：把向量召回和关键词召回合并。
 *
 * <p>这里使用 Reciprocal Rank Fusion 的简化版本。它不要求不同检索器分数同尺度，
 * 只看各自排名，因此比直接把 BM25 分和向量分相加更稳。
 */
public final class HybridRetriever {

    private final VectorRetriever vectorRetriever;
    private final KeywordRetriever keywordRetriever;

    public HybridRetriever(VectorRetriever vectorRetriever, KeywordRetriever keywordRetriever) {
        this.vectorRetriever = vectorRetriever;
        this.keywordRetriever = keywordRetriever;
    }

    public List<SearchHit> search(String query, String tenantId, int topK) {
        Map<String, CandidateScore> scores = new LinkedHashMap<>();
        merge(scores, vectorRetriever.search(query, tenantId, Math.max(topK, 5)), "vector");
        merge(scores, keywordRetriever.search(query, tenantId, Math.max(topK, 5)), "keyword");

        return scores.values().stream()
            .map(candidate -> new SearchHit(candidate.chunk, candidate.score, candidate.reason()))
            .sorted(Comparator.comparingDouble(SearchHit::score).reversed())
            .limit(Math.max(1, topK))
            .toList();
    }

    private void merge(Map<String, CandidateScore> scores, List<SearchHit> hits, String source) {
        for (int i = 0; i < hits.size(); i++) {
            SearchHit hit = hits.get(i);
            CandidateScore candidate = scores.computeIfAbsent(
                hit.chunk().id(),
                id -> new CandidateScore(hit.chunk())
            );

            /*
             * RRF 常用 1 / (k + rank)。k 越大，头部排名优势越平滑。
             * demo 数据很小，这里用 20 即可。
             */
            candidate.score += 1.0 / (20 + i + 1);
            candidate.sources.add(source);
        }
    }

    private static final class CandidateScore {
        private final KnowledgeChunk chunk;
        private final List<String> sources = new ArrayList<>();
        private double score;

        private CandidateScore(KnowledgeChunk chunk) {
            this.chunk = chunk;
        }

        private String reason() {
            return "hybrid:" + String.join("+", sources);
        }
    }
}
