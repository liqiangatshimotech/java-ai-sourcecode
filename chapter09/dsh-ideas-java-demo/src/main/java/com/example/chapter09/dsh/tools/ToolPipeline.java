package com.example.chapter09.dsh.tools;

import com.example.chapter09.dsh.event.SessionLog;
import com.example.chapter09.dsh.plugin.PluginContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具执行流水线：对应 dsh 的 tool-execution-pipeline。
 *
 * 一次调用按固定顺序走五段：
 * 1. tools/pre-execute（waterfall）：钩子、权限策略、沙箱——可 allow/deny/ask，
 *    拥有决策权的监听器直接返回，短路是设计意图；
 * 2. 审批：ask 的调用经 ctx.approval 问一次，没人回答或被拒绝都按拒绝处理；
 * 3. 单调守卫（serial）：只能拒绝或弃权，用来放“身份受保护的硬规则”；
 * 4. tools/execute（环绕链）：超时、重试、指标这类横切关注点包在最外层，
 *    链尾是工具主体本身；任何一段抛异常都会被规范化成 isError 结果；
 * 5. tools/post-execute（环绕链）：可接受、阻断、替换结果——
 *    被拒绝的调用也会走到这里（跳过主体）。
 *
 * 结算完成后写两条轨迹：session 里的 tool/result 事件（模型可见），
 * 和同步的 tools/result 通知（UI / 遥测可见）。
 */
public final class ToolPipeline {

    /** pre-execute 的三种裁决。 */
    public enum Decision { ALLOW, DENY, ASK }

    /** pre-execute 监听器的返回值：DENY / ASK 携带原因。 */
    public record PreExecuteDecision(Decision decision, String reason) {
        public static PreExecuteDecision allow() {
            return new PreExecuteDecision(Decision.ALLOW, null);
        }

        public static PreExecuteDecision deny(String reason) {
            return new PreExecuteDecision(Decision.DENY, reason);
        }

        public static PreExecuteDecision ask(String reason) {
            return new PreExecuteDecision(Decision.ASK, reason);
        }
    }

    /** 面向模型的工具结果。 */
    public record ToolResult(String tool, String output, boolean error) {
    }

    /** execute 阶段的环绕监听器：包超时/重试/指标，链尾是工具主体。 */
    @FunctionalInterface
    public interface ExecuteWrapper {
        String around(ToolCall call, ToolBodyNext next) throws Exception;
    }

    /** execute 环绕链的“下一棒”。 */
    public interface ToolBodyNext {
        String invoke() throws Exception;
    }

    /** post-execute 阶段的改写器：可替换输出文本。 */
    @FunctionalInterface
    public interface PostProcessor {
        String process(ToolCall call, String output, boolean error);
    }

    private final PluginContext ctx;
    private final ToolRegistry registry;
    private final List<ExecuteWrapper> executeWrappers = new ArrayList<>();
    private final List<PostProcessor> postProcessors = new ArrayList<>();

    public ToolPipeline(PluginContext ctx, ToolRegistry registry) {
        this.ctx = ctx;
        this.registry = registry;
    }

    /** 注册 execute 环绕层：先注册的在外层（超时应该在最外、重试在其内）。 */
    public void addExecuteWrapper(ExecuteWrapper wrapper) {
        executeWrappers.add(wrapper);
        ctx.effect(() -> executeWrappers.remove(wrapper));
    }

    /** 注册 post-execute 改写层。 */
    public void addPostProcessor(PostProcessor processor) {
        postProcessors.add(processor);
        ctx.effect(() -> postProcessors.remove(processor));
    }

    public ToolRegistry registry() {
        return registry;
    }

    /** 工具注册的便捷入口：注册即登记 disposer，插件卸载时自动撤销。 */
    public void registerTool(ToolDefinition definition) {
        registry.register(definition);
        ctx.effect(() -> registry.unregister(definition.name()));
    }

    /** 执行一次工具调用，完整走五段流水线并结算结果。 */
    public ToolResult execute(ToolCall call, SessionLog session) {
        // 进入流水线先留痕：tool/call 是会话事件，UI 靠它渲染 pending 卡片。
        Map<String, Object> callPayload = new LinkedHashMap<>();
        callPayload.put("tool", call.tool());
        callPayload.put("args", call.args());
        session.append("tool/call", callPayload);

        String deniedReason = null;

        // 1) pre-execute：权限策略 / 沙箱钩子在这里裁决。
        PreExecuteDecision decision =
                (PreExecuteDecision) ctx.bus().waterfall("tools/pre-execute", call);
        if (decision == null) {
            decision = PreExecuteDecision.allow();
        }
        if (decision.decision() == Decision.DENY) {
            deniedReason = "policy denied: " + decision.reason();
        } else if (decision.decision() == Decision.ASK) {
            // 2) 审批接缝：demo 里是控制台，真实 dsh 是 Web UI / ACP 客户端。
            boolean approved = askApproval(call, decision.reason());
            if (!approved) {
                deniedReason = "approval " + (approvalAvailable() ? "denied" : "unavailable");
            }
        }

        // 3) 单调守卫：只能 deny 或弃权，不重排、不可绕过。
        if (deniedReason == null) {
            Object guardReason = ctx.bus().serial("tools/monotonic-guard", call);
            if (guardReason != null) {
                deniedReason = "guard denied: " + guardReason;
            }
        }

        String output;
        boolean isError;
        if (deniedReason != null) {
            // 拒绝路径：跳过工具主体，但仍会经过 post-execute。
            output = deniedReason;
            isError = true;
        } else {
            // 4) execute 环绕链 + 工具主体；异常统一规范化为 isError。
            try {
                output = runExecuteChain(call);
                isError = false;
            } catch (Exception e) {
                output = e.getClass().getSimpleName() + ": " + e.getMessage();
                isError = true;
            }
        }

        // 5) post-execute：最后的统一出口，改写结果或追加说明。
        for (PostProcessor processor : new ArrayList<>(postProcessors)) {
            output = processor.process(call, output, isError);
        }

        // 结算：tool/result 进会话（模型可见即已记录），tools/result 同步广播。
        Map<String, Object> resultPayload = new LinkedHashMap<>();
        resultPayload.put("tool", call.tool());
        resultPayload.put("output", output);
        resultPayload.put("isError", isError);
        session.append("tool/result", resultPayload);

        ToolResult result = new ToolResult(call.tool(), output, isError);
        ctx.bus().emit("tools/result", result);
        return result;
    }

    /** execute 环绕链：外层包装依次向内传递，链尾执行工具主体。 */
    private String runExecuteChain(ToolCall call) throws Exception {
        return chainExecute(executeWrappers, 0, call);
    }

    private String chainExecute(List<ExecuteWrapper> wrappers, int index, ToolCall call) throws Exception {
        if (index >= wrappers.size()) {
            return invokeToolBody(call);
        }
        return wrappers.get(index).around(call, () -> chainExecute(wrappers, index + 1, call));
    }

    /** 链尾：查注册表并执行工具主体；找不到工具按错误规范化。 */
    private String invokeToolBody(ToolCall call) throws Exception {
        ToolDefinition definition = registry.find(call.tool())
                .orElseThrow(() -> new IllegalStateException("unknown tool: " + call.tool()));
        return definition.body().execute(call, ctx);
    }

    private boolean approvalAvailable() {
        try {
            ctx.service("approval", com.example.chapter09.dsh.seam.ApprovalSeam.class);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private boolean askApproval(ToolCall call, String reason) {
        if (!approvalAvailable()) {
            return false; // 没有审批方回答 → unavailable → 拒绝，和 dsh 一致
        }
        return ctx.service("approval", com.example.chapter09.dsh.seam.ApprovalSeam.class)
                .ask("Allow tool: " + call.tool() + " (" + reason + ")", call.toString());
    }
}
