package com.example.chapter09.dsh.core;

import com.example.chapter09.dsh.plugin.Plugin;
import com.example.chapter09.dsh.plugin.PluginContext;

/**
 * 基础 bundle：对应 dsh 的 dsh-base 组合包（模型适配器、工具、持久化、
 * 沙箱、审批、设置、凭据、遥测都在这个层）。
 *
 * demo 里把几个最小核心插件按依赖顺序挂上来，模拟
 * “entry → bundle → patch” 的插件树叠加。
 */
public final class BaseBundle {

    private BaseBundle() {
    }

    /** core/session：提供会话日志服务。 */
    public static final Plugin SESSION = ctx -> {
        SessionService sessions = new SessionService(ctx);
        ctx.provideService("sessions", sessions);
    };

    /** core/system-prompt：提供提示词组装服务。 */
    public static final Plugin SYSTEM_PROMPT = ctx -> {
        SystemPromptService prompt = new SystemPromptService();
        ctx.provideService("systemPrompt", prompt);
    };

    /** core/tools：提供工具注册表 + 执行流水线。 */
    public static final Plugin TOOLS = ctx -> {
        com.example.chapter09.dsh.tools.ToolRegistry registry =
                new com.example.chapter09.dsh.tools.ToolRegistry();
        com.example.chapter09.dsh.tools.ToolPipeline pipeline =
                new com.example.chapter09.dsh.tools.ToolPipeline(ctx, registry);
        ctx.provideService("tools", pipeline);
    };

    /** core/agent-loop：默认驱动器。它也只是个插件，可被整体替换。 */
    public static final Plugin AGENT_LOOP = ctx -> {
        com.example.chapter09.dsh.agent.AgentLoop loop =
                new com.example.chapter09.dsh.agent.AgentLoop(ctx);
        ctx.provideService("agentLoop", loop);
    };

    /** 按顺序挂载基础 bundle。 */
    public static void load(PluginContext ctx) {
        ctx.load("core/session", SESSION);
        ctx.load("core/system-prompt", SYSTEM_PROMPT);
        ctx.load("core/tools", TOOLS);
        ctx.load("core/agent-loop", AGENT_LOOP);
    }
}
