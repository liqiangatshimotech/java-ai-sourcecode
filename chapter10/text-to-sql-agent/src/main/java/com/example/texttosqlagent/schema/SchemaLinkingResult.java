package com.example.texttosqlagent.schema;

import java.util.List;

/**
 * Schema Linking 的结果。
 *
 * @param selectedTables  选中的相关表（带完整元数据）
 * @param matchedKeywords 命中的关键词明细（表名 -> 命中词列表），用于审计和调试
 * @param schemaContext   拼好的 schema 上下文（DDL 片段 + 指标口径），直接喂给模型
 */
public record SchemaLinkingResult(
    List<TableSchema> selectedTables,
    java.util.Map<String, List<String>> matchedKeywords,
    String schemaContext
) {
}
