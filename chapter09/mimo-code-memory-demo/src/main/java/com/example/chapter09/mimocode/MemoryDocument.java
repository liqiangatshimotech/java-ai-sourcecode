package com.example.chapter09.mimocode;

import java.nio.file.Path;

/**
 * 被索引的一份记忆文档。scope 用来区分 global/project/session/task，
 * type 用来区分 MEMORY、checkpoint、progress 等材料。
 */
public record MemoryDocument(
        String id,
        String scope,
        String type,
        Path path,
        String body
) {
}
