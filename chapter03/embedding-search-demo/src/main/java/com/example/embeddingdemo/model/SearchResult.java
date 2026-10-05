package com.example.embeddingdemo.model;

/**
 * 检索结果。
 *
 * <p>score 在关键词检索里表示命中的关键词比例，在 Embedding 检索里表示余弦相似度。
 * 同一个字段在不同检索策略下含义不同，实际项目里可以用更明确的子类型拆开。
 */
public record SearchResult(
    KnowledgeEntry entry,
    double score
) {
}
