package com.example.codingagent.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册表是模型能做什么的白名单。
 * 没有注册的工具不能被调用，避免模型越权执行任意动作。
 */
public final class ToolRegistry {

    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public ToolRegistry register(AgentTool tool) {
        tools.put(tool.name(), tool);
        return this;
    }

    public AgentTool require(String name) {
        AgentTool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Tool is not registered: " + name);
        }
        return tool;
    }

    public List<String> names() {
        return List.copyOf(tools.keySet());
    }
}
