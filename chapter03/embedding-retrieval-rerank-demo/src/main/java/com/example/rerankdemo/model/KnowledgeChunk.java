package com.example.rerankdemo.model;

import java.util.List;

/**
 * RAG 检索阶段真正处理的通常不是整篇文档，而是切分后的 Chunk。
 *
 * <p>这个 record 保留了 tenantId、source、tags 等字段，是为了强调一个工程事实：
 * 召回优化不只是算向量相似度，还要处理租户隔离、来源追踪和业务标签过滤。
 */
public record KnowledgeChunk(
    String id,
    String tenantId,
    String title,
    String text,
    String source,
    List<String> tags
) {

    /**
     * 向量化时把标题和正文一起放进去。
     *
     * <p>标题经常包含强业务意图，比如“发票抬头错误”；正文包含处理规则。
     * 只嵌入正文会损失主题，只嵌入标题会损失操作细节。
     */
    public String searchableText() {
        return title + "\n" + text;
    }
}
