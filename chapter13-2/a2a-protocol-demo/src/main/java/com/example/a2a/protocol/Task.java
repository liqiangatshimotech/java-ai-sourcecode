package com.example.a2a.protocol;

import java.util.List;
import java.util.Map;

/**
 * A2A 的核心动作单元。
 */
public record Task(
        String id,
        String contextId,
        TaskStatus status,
        List<Artifact> artifacts,
        List<Message> history,
        Map<String, Object> metadata) {}
