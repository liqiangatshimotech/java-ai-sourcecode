package com.example.rerankdemo.model;

/**
 * 一条检索或重排结果。
 *
 * <p>reason 是为了让 demo 输出可解释。真实系统里也建议记录召回来源、原始分数、
 * rerank 分数和过滤原因，否则线上问题很难定位。
 */
public record SearchHit(
    KnowledgeChunk chunk,
    double score,
    String reason
) {
}
