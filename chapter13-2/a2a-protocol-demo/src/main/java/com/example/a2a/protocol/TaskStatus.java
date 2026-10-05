package com.example.a2a.protocol;

import java.time.Instant;

/**
 * Task 的当前状态。
 */
public record TaskStatus(
        TaskState state,
        Message message,
        Instant timestamp) {

    public static TaskStatus of(TaskState state, Message message) {
        return new TaskStatus(state, message, Instant.now());
    }
}
