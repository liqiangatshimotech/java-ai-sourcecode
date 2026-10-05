package com.example.chapter09.dsh.tools;

import com.example.chapter09.dsh.plugin.PluginContext;

import java.util.Map;

/**
 * 工具定义：对应 dsh 里注册进 ctx.tools 的一个工具。
 *
 * 工具主体只写业务逻辑：读哪个接缝、做什么事、返回什么内容。
 * 超时、重试、权限、审批、结果改写全部在流水线的 waterfall 里，
 * 由别的插件叠加——工具不和策略服务耦合，策略可以跨工具复用。
 */
public record ToolDefinition(
        String name,
        String description,
        Map<String, String> parameterDocs,
        ToolBody body
) {

    /** 工具主体：拿到调用参数和 ctx，返回给模型看的结果文本。 */
    @FunctionalInterface
    public interface ToolBody {
        String execute(ToolCall call, PluginContext ctx) throws Exception;
    }
}
