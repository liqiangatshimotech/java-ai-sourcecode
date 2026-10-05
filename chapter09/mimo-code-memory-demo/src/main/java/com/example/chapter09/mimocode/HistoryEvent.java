package com.example.chapter09.mimocode;

import java.time.Instant;

/**
 * 原始轨迹中的一条事件。MiMo-Code 里的 history_fts 会保存消息、
 * 工具调用、工具结果等内容。这里用 Java record 模拟这个事实账本。
 */
public record HistoryEvent(
        long id,
        String sessionId,
        EventKind kind,
        String title,
        String body,
        Instant createdAt
) {
    public enum EventKind {
        USER_MESSAGE,
        ASSISTANT_MESSAGE,
        TOOL_CALL,
        TOOL_RESULT,
        CHECKPOINT,
        DREAM,
        DISTILL
    }
}
