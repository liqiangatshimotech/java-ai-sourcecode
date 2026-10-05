package com.example.chapter09.dsh.core;

import com.example.chapter09.dsh.plugin.PluginContext;
import com.example.chapter09.dsh.tools.ToolCall;
import com.example.chapter09.dsh.tools.ToolPipeline;

/**
 * 流水线上的策略层：审批策略、单调守卫、超时包装、结果改写。
 *
 * 这些都是独立插件，不改动任何工具代码就叠进流水线——
 * 对应 dsh “钩子可跨工具系列复用，工具不与策略服务耦合”的设计意图。
 */
public final class PipelinePolicies {

    private PipelinePolicies() {
    }

    /**
     * pre-execute 策略：shell 类工具一律 ASK。
     * 拥有决策权的监听器直接返回、不调 next()——短路即裁决。
     */
    public static void askApprovalForShell(PluginContext ctx) {
        ctx.onWaterfall("tools/pre-execute", (args, next) -> {
            ToolCall call = (ToolCall) args[0];
            if ("shell".equals(call.tool())) {
                return ToolPipeline.PreExecuteDecision.ask("shell affects the machine");
            }
            return next.call(); // 观察型路径：改完或不改都往下游传
        });
    }

    /**
     * 单调守卫：密钥文件路径一律拒绝。
     * 守卫只能 deny 或弃权（返回 null），这是它和普通策略的区别：
     * 身份受保护、不可被下游覆盖。
     */
    public static void protectSecrets(PluginContext ctx) {
        ctx.onSerial("tools/monotonic-guard", args -> {
            ToolCall call = (ToolCall) args[0];
            String path = call.args().getOrDefault("path", "");
            if (path.contains(".env") || path.contains("credentials")) {
                return "protected path: " + path;
            }
            return null; // 弃权
        });
    }

    /** execute 环绕层：模拟超时/计时（真实实现在这里包 timeout 和 retry）。 */
    public static void timeToolCalls(PluginContext ctx) {
        ToolPipeline pipeline = ctx.service("tools", ToolPipeline.class);
        pipeline.addExecuteWrapper((call, next) -> {
            long start = System.nanoTime();
            try {
                return next.invoke();
            } finally {
                long costMs = (System.nanoTime() - start) / 1_000_000;
                ctx.bus().emit("telemetry/tool", call.tool() + " took " + costMs + "ms");
            }
        });
    }

    /**
     * post-execute 改写层：给错误结果追加固定说明。
     * 注意被拒绝的调用也会经过这里——dsh 特意保证 post 阶段
     * 是“所有调用的统一出口”。
     */
    public static void annotateErrors(PluginContext ctx) {
        ToolPipeline pipeline = ctx.service("tools", ToolPipeline.class);
        pipeline.addPostProcessor((call, output, error) ->
                error ? "[blocked] " + output : output);
    }
}
