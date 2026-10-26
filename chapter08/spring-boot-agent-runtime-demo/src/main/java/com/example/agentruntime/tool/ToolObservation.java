package com.example.agentruntime.tool;

import java.util.Map;
/** 成功、失败与权限阻塞使用同一观察协议，失败不注入成功事实。 */
public record ToolObservation(String status, String content, Map<String, String> facts) {
    public ToolObservation { facts = Map.copyOf(facts); }
    public static ToolObservation success(String content, Map<String, String> facts) {
        return new ToolObservation("SUCCESS", content, facts);
    }
    public static ToolObservation blocked(String content, Map<String, String> facts) {
        return new ToolObservation("BLOCKED", content, facts);
    }
    public static ToolObservation failed(String content) {
        return new ToolObservation("FAILED", content, Map.of());
    }
}
