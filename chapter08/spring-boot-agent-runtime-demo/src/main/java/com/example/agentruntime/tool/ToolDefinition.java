package com.example.agentruntime.tool;

import java.util.function.Function;
/** 注册时把能力、风险和 handler 绑定在一起，调用时统一检查。 */
public record ToolDefinition(String name, String description, ToolRisk risk,
        Function<ToolCall, ToolObservation> handler) {}
