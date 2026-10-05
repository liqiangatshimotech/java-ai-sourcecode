package com.example.embeddingdemo.model;

/**
 * 一条可以被检索的业务知识。
 *
 * <p>真实知识库里通常还会有租户、来源 URL、更新时间、权限标签等 metadata。
 * 这个 demo 先保留最小字段，重点放在“同一段文本如何从字符串变成向量”。
 */
public record KnowledgeEntry(
    String id,
    String title,
    String content
) {

    /**
     * Embedding 时不要只放标题。
     *
     * <p>标题提供主题，正文提供业务细节。两者合在一起做向量，检索效果通常比只嵌入标题稳定。
     */
    public String searchableText() {
        return title + "\n" + content;
    }
}
