package com.example.aicustomerservice.service;

import com.example.aicustomerservice.dao.KnowledgeDao;
import com.example.aicustomerservice.dao.KnowledgeVectorDao;
import com.example.aicustomerservice.entity.KnowledgeChunk;
import com.example.aicustomerservice.entity.RetrievedKnowledge;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * RAG 检索服务。
 *
 * 默认生产路径使用 PostgreSQL + pgvector；内存模式保留关键词路径，方便单元测试和离线演示。
 */
public class RetrievalService {

    private final KnowledgeDao knowledgeDao;
    private final SemanticChunker chunker;
    private final KnowledgeVectorDao vectorDao;

    public RetrievalService(KnowledgeDao knowledgeDao, SemanticChunker chunker) {
        this.knowledgeDao = knowledgeDao;
        this.chunker = chunker;
        this.vectorDao = null;
    }

    public RetrievalService(KnowledgeVectorDao vectorDao) {
        this.knowledgeDao = null;
        this.chunker = null;
        this.vectorDao = vectorDao;
    }

    public List<RetrievedKnowledge> search(String query, int limit) {
        if (vectorDao != null) {
            return vectorDao.search(query, limit);
        }
        Set<String> queryTokens = tokenize(query);
        return knowledgeDao.findPublishedDocuments()
            .stream()
            .flatMap(document -> chunker.split(document).stream())
            .map(chunk -> score(chunk, queryTokens))
            .filter(result -> result.score() > 0)
            .sorted(Comparator.comparing(RetrievedKnowledge::score).reversed())
            .limit(limit)
            .toList();
    }

    private RetrievedKnowledge score(KnowledgeChunk chunk, Set<String> queryTokens) {
        Set<String> textTokens = tokenize(chunk.title() + " " + chunk.text());
        long overlap = queryTokens.stream().filter(textTokens::contains).count();

        // 中文短词很难完全分词。这里额外用子串命中补分，保持演示效果稳定。
        double substringBoost = queryTokens.stream()
            .filter(token -> token.length() >= 2 && chunk.text().contains(token))
            .count() * 0.2;

        double score = queryTokens.isEmpty() ? 0 : overlap / (double) queryTokens.size() + substringBoost;
        return new RetrievedKnowledge(
            chunk.chunkId(),
            chunk.documentId(),
            chunk.version(),
            chunk.title(),
            chunk.text(),
            Math.min(score, 1.0)
        );
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String normalized = text.toLowerCase(Locale.ROOT)
            .replaceAll("[，。！？；：、,.!?;:()（）\\[\\]{}]", " ");
        Set<String> tokens = new LinkedHashSet<>(Arrays.asList(normalized.split("\\s+")));
        for (String keyword : List.of("退款", "退货", "物流", "发票", "订单", "售后", "企业", "VIP", "超时", "工单")) {
            if (text.contains(keyword)) {
                tokens.add(keyword.toLowerCase(Locale.ROOT));
            }
        }
        tokens.remove("");
        return tokens;
    }
}
