package com.example.chapter09.dsh.agent;

import com.example.chapter09.dsh.core.SystemPromptService;
import com.example.chapter09.dsh.event.SessionLog;
import com.example.chapter09.dsh.llm.LlmSeam;
import com.example.chapter09.dsh.plugin.PluginContext;
import com.example.chapter09.dsh.tools.ToolCall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认 Agent 循环：对应 dsh 的 core/agent-loop 包（ctx.agentLoop）。
 *
 * 注意它自己也只是“默认驱动器”——一个插件提供的实现，
 * 理论上可以被别的 loop 插件整体替换，这就是“没有特权内核”的含义。
 *
 * 术语对齐 dsh：
 * - step：一次模型请求 + 它触发的工具调用；
 * - turn：零到多个 step，直到模型自然停止且没有排队输入。
 */
public final class AgentLoop {

    /** agent/request 环绕层：记录日志、限流等横切关注点。 */
    @FunctionalInterface
    public interface RequestWrapper {
        LlmSeam.LlmReply around(String systemPrompt, List<Message> messages, RequestNext next);
    }

    public interface RequestNext {
        LlmSeam.LlmReply invoke();
    }

    private final PluginContext ctx;
    private final List<RequestWrapper> requestWrappers = new ArrayList<>();

    public AgentLoop(PluginContext ctx) {
        this.ctx = ctx;
    }

    public void addRequestWrapper(RequestWrapper wrapper) {
        requestWrappers.add(wrapper);
        ctx.effect(() -> requestWrappers.remove(wrapper));
    }

    /**
     * 跑一个轮次：从认领输入到 turn/end 的完整事件链。
     *
     * 链条（对照 dsh 文档）：agent/inbox/inserted → agent/status(running)
     * → turn/start → user/message → agent/pre-step → step/start
     * → system-prompt/assemble → agent/request → assistant/message
     * → tool/call / tool/result（循环到没有新工具调用）
     * → agent/turn-stopping（serial 终止检查点）→ turn/end → agent/status(idle)。
     */
    public void runTurn(SessionLog session, String userInput) {
        ctx.bus().emit("agent/inbox/inserted", userInput);
        ctx.bus().emit("agent/status", "running");
        session.append("turn/start", Map.of("session", session.sessionId()));

        // 认领输入：从这一刻起它成为模型可见的事实，必须先进日志。
        Map<String, Object> userPayload = new LinkedHashMap<>();
        userPayload.put("text", userInput);
        session.append("user/message", userPayload);

        // pre-step waterfall：拥有权威否决权的钩子可以直接拒绝这个步骤。
        Object preStep = ctx.bus().waterfall("agent/pre-step", session);
        if ("REJECT".equals(preStep)) {
            // 被拒的轮次仍然要关闭：保证日志里 turn/start 和 turn/end 成对。
            session.append("turn/end", Map.of("reason", "pre-step rejected"));
            ctx.bus().emit("agent/status", "idle");
            return;
        }

        boolean stopped = false;
        while (!stopped) {
            session.append("step/start", Map.of());

            // 组装系统提示：片段来自各插件的可逆注册，工具清单来自注册表。
            String systemPrompt = assembleSystemPrompt();
            ctx.bus().emit("system-prompt/assemble", systemPrompt);

            // 模型请求：只喂 deriveMessages() 的投影结果——模型可见即已记录。
            LlmSeam.LlmReply reply = runRequestChain(systemPrompt, session.deriveMessages());

            Map<String, Object> assistantPayload = new LinkedHashMap<>();
            assistantPayload.put("text", reply.text());
            assistantPayload.put("provider", ctx.service("llm", LlmSeam.class).providerName());
            session.append("assistant/message", assistantPayload);

            if (!reply.toolCalls().isEmpty()) {
                // 有工具调用：逐个走流水线，然后进入下一个 step 消化结果。
                for (ToolCall call : reply.toolCalls()) {
                    ctx.service("tools", com.example.chapter09.dsh.tools.ToolPipeline.class)
                            .execute(call, session);
                }
                session.append("step/end", Map.of("outcome", "tool-calls"));
                continue;
            }

            session.append("step/end", Map.of("outcome", "final-answer"));

            // 自然停止 → serial 终止检查点：Goal gate 这类裁判可以投否决票。
            Object veto = ctx.bus().serial("agent/turn-stopping", session);
            if (Boolean.TRUE.equals(veto)) {
                continue;
            }
            stopped = true;
        }

        session.append("turn/end", Map.of("reason", "natural stop"));
        ctx.bus().emit("agent/status", "idle");
    }

    private String assembleSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append(ctx.service("systemPrompt", SystemPromptService.class).assemble());
        sb.append("\nAvailable tools:\n");
        sb.append(ctx.service("tools", com.example.chapter09.dsh.tools.ToolPipeline.class)
                .registry().describeAll());
        return sb.toString();
    }

    /** agent/request 环绕链，链尾是 LLM 接缝。 */
    private LlmSeam.LlmReply runRequestChain(String systemPrompt, List<Message> messages) {
        return chainRequest(0, systemPrompt, messages);
    }

    private LlmSeam.LlmReply chainRequest(int index, String systemPrompt, List<Message> messages) {
        if (index >= requestWrappers.size()) {
            return ctx.service("llm", LlmSeam.class).complete(systemPrompt, messages);
        }
        return requestWrappers.get(index)
                .around(systemPrompt, messages, () -> chainRequest(index + 1, systemPrompt, messages));
    }
}
