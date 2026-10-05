package com.example.chapter09.mimocode.context;

import java.nio.file.Path;

/**
 * checkpoint 文件和它覆盖的边界。boundaryIndex 表示从哪条消息开始保留原文，
 * 它之前的历史已经被写进 checkpoint.md，后面的近期消息会进入下一轮上下文。
 */
public record CheckpointSnapshot(
        int boundaryIndex,
        int coveredMessages,
        int coveredTokens,
        Path checkpointFile
) {
}
