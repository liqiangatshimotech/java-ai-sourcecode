package com.example.chapter09.dsh.tools;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 工具注册表：对应 dsh 的 core/tools 包（ctx.tools）。
 *
 * 注册走 ctx.effect，是可逆副作用：提供工具的插件被卸载时，
 * 它的工具自动从注册表消失。
 */
public final class ToolRegistry {

    private final Map<String, ToolDefinition> tools = new LinkedHashMap<>();

    public void register(ToolDefinition definition) {
        tools.put(definition.name(), definition);
    }

    public void unregister(String name) {
        tools.remove(name);
    }

    public Optional<ToolDefinition> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /** 组装进系统提示词用的清单：名字 + 说明 + 参数。 */
    public String describeAll() {
        StringBuilder sb = new StringBuilder();
        for (ToolDefinition tool : tools.values()) {
            sb.append("- ").append(tool.name()).append(": ").append(tool.description()).append('\n');
            for (Map.Entry<String, String> param : tool.parameterDocs().entrySet()) {
                sb.append("    * ").append(param.getKey()).append(" - ").append(param.getValue()).append('\n');
            }
        }
        return sb.toString();
    }
}
