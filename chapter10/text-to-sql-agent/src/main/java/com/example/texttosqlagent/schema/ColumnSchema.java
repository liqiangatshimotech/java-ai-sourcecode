package com.example.texttosqlagent.schema;

/**
 * 一列的元数据：列名、类型、中文注释。
 *
 * comment 来自 schema.sql 里的 COMMENT ON COLUMN，
 * 是 Schema Linking 打分和 prompt 拼装的原料。
 */
public record ColumnSchema(String name, String type, String comment) {
}
