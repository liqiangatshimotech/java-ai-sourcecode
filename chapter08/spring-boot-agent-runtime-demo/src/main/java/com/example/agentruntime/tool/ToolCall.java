package com.example.agentruntime.tool;

import java.util.Map;
/** 冻结动作名称和参数，审批期间不能通过修改 Map 偷换待执行动作。 */
public record ToolCall(String toolName, Map<String, String> arguments) {
    public ToolCall {
        if (toolName == null || toolName.isBlank()) throw new IllegalArgumentException("工具名不能为空");
        arguments = Map.copyOf(arguments);
    }
}
