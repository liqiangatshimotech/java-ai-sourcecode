package com.example.aiproduction.observability;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 一次 Agent 请求的执行轨迹。
 *
 * <p>TraceId 是生产排查的主线。模型请求、工具调用、成本估算、安全检查都应该挂在同一个 traceId 下，排查时才能串起来。
 */
public class AgentTrace {

    private final String traceId;
    private final List<TraceEvent> events = new ArrayList<>();

    public AgentTrace(String traceId) {
        this.traceId = traceId;
    }

    public void add(String type, String message) {
        add(type, message, Map.of());
    }

    public void add(String type, String message, Map<String, String> metadata) {
        events.add(new TraceEvent(traceId, type, message, metadata, Instant.now()));
    }

    public String traceId() {
        return traceId;
    }

    public List<TraceEvent> events() {
        return List.copyOf(events);
    }
}
