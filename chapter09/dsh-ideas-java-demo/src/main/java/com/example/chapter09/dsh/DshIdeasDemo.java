package com.example.chapter09.dsh;

import com.example.chapter09.dsh.agent.AgentLoop;
import com.example.chapter09.dsh.agent.Message;
import com.example.chapter09.dsh.core.BaseBundle;
import com.example.chapter09.dsh.core.GoalGatePlugin;
import com.example.chapter09.dsh.core.GuidelinePlugin;
import com.example.chapter09.dsh.core.PipelinePolicies;
import com.example.chapter09.dsh.core.SeamProviders;
import com.example.chapter09.dsh.core.SessionService;
import com.example.chapter09.dsh.core.SystemPromptService;
import com.example.chapter09.dsh.core.WorkspaceTools;
import com.example.chapter09.dsh.event.SessionLog;
import com.example.chapter09.dsh.llm.AiProviderConfig;
import com.example.chapter09.dsh.llm.LlmSeam;
import com.example.chapter09.dsh.llm.ReplayLlmProvider;
import com.example.chapter09.dsh.plugin.PluginContext;
import com.example.chapter09.dsh.tools.ToolCall;
import com.example.chapter09.dsh.tools.ToolPipeline;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * dsh-ideas-java-demo 入口：用三个“幕”演示 DeepSeek Harness 的三个核心设计。
 *
 * 第一幕（对应 9.7.2）：插件树 + 可逆注册——挂载 team-guideline 插件再卸载，
 *   观察提示词片段、工具、遥测监听一起消失；
 * 第二幕（对应 9.7.3）：会话事件日志——跑一个完整 turn，
 *   打印追加事件流、投影消息、压缩投影、fork 子会话；
 * 第三幕（对应 9.7.4）：能力接缝——fs 从 local 换到 sandbox，
 *   消费方零改动；审批 ask、单调守卫、超时包装、错误改写逐段生效。
 *
 * 全程使用 llm-replay 离线回放，不需要任何 API Key。
 */
public final class DshIdeasDemo {

    public static void main(String[] args) throws Exception {
        AiProviderConfig config = AiProviderConfig.fromEnvironment(System.getenv());
        banner("模型配置（课程约定：默认 DeepSeek，保留 Ollama / ChatGPT 入口）");
        System.out.printf("  provider=%s model=%s baseUrl=%s apiKey=%s%n",
                config.provider(), config.model(), config.baseUrl(), config.maskedApiKey());
        System.out.println("  本 demo 默认使用 llm-replay 离线回放，不发起真实模型请求");

        Path workspace = Files.createTempDirectory("dsh-demo-workspace");
        System.out.println("  workspace=" + workspace);

        PluginContext ctx = new PluginContext();

        // ===== 第一幕：插件树与可逆注册 =====
        banner("第一幕：插件树 —— 一切皆插件，注册皆可逆");
        BaseBundle.load(ctx);
        ctx.load("fs-local", SeamProviders.localFs());
        ctx.load("workspace-tools", c -> WorkspaceTools.register(c, workspace));
        System.out.println("  插件树: " + ctx.pluginTree());
        System.out.println("  ctx 服务: " + ctx.serviceKeys());

        GuidelinePlugin.load(ctx);
        System.out.println("\n  挂载 team-guideline 之后:");
        printPromptAndTools(ctx);

        ctx.unload("team-guideline");
        System.out.println("\n  卸载 team-guideline 之后（disposer 逆序执行）:");
        printPromptAndTools(ctx);

        // ===== 第二幕：会话事件日志 =====
        banner("第二幕：模型可见即已记录 —— 会话日志与投影");
        ReplayLlmProvider replay = new ReplayLlmProvider(List.of(
                LlmSeam.LlmReply.toolCalls(List.of(
                        ToolCall.of("write_file", "path", "safe/notes.md", "content", "1. 拉取镜像\n2. 滚动重启"))),
                LlmSeam.LlmReply.text("部署说明已写入，不过我还想再核对一遍清单。"),
                LlmSeam.LlmReply.text("清单核对完毕，任务完成。DONE")));
        ctx.load("llm-replay", SeamProviders.llm(replay));
        GoalGatePlugin.load(ctx);

        SessionLog session = ctx.service("sessions", SessionService.class).create("s-main");
        AgentLoop loop = ctx.service("agentLoop", AgentLoop.class);
        loop.runTurn(session, "把部署说明写到 safe/notes.md，并确认内容完整");

        System.out.println("\n  会话事件（只追加，永不改写）:");
        session.events().forEach(e -> System.out.println("    " + e));

        System.out.println("\n  deriveMessages() 投影（模型实际看到的历史）:");
        printDerived(session.deriveMessages());

        session.compact("用户要求写部署说明；write_file 已写入 safe/notes.md；清单已核对");
        System.out.println("\n  compact 之后的历史（旧事件折叠成一条摘要，原始事件仍在日志里）:");
        printDerived(session.deriveMessages());

        SessionLog child = SessionLog.fork(session, session.lastSeq(), "s-child");
        child.append("user/message", java.util.Map.of("text", "把清单再补一节回滚步骤"));
        System.out.println("\n  fork 出的子会话 s-child 继承边界前全部事实，之后独立追加:");
        printDerived(child.deriveMessages());

        // ===== 第三幕：能力接缝与流水线 =====
        banner("第三幕：Capability Seam —— 换实现不换消费者");
        ctx.load("approval", SeamProviders.approval((title, detail) -> {
            System.out.println("    [approval] " + title + " <- " + detail);
            System.out.println("    [approval] 用户选择：允许");
            return true;
        }));
        ctx.load("pipeline-policies", c -> {
            PipelinePolicies.askApprovalForShell(c);
            PipelinePolicies.protectSecrets(c);
            PipelinePolicies.timeToolCalls(c);
            PipelinePolicies.annotateErrors(c);
        });
        ToolPipeline pipeline = ctx.service("tools", ToolPipeline.class);
        SessionLog session3 = ctx.service("sessions", SessionService.class).create("s-seams");

        System.out.println("\n  当前 fs 提供方: "
                + ctx.service("fs", com.example.chapter09.dsh.seam.FileSystemSeam.class).providerName());
        runCall(pipeline, session3,
                ToolCall.of("write_file", "path", "../escape-local.txt", "content", "should pass on local"));

        ctx.unload("fs-local");
        ctx.load("fs-sandbox", SeamProviders.sandboxedFs(workspace));
        System.out.println("\n  已替换 fs 提供方为: "
                + ctx.service("fs", com.example.chapter09.dsh.seam.FileSystemSeam.class).providerName()
                + "（write_file 工具的代码一行没动）");
        runCall(pipeline, session3,
                ToolCall.of("write_file", "path", "../escape.txt", "content", "should be denied"));
        runCall(pipeline, session3,
                ToolCall.of("write_file", "path", "safe/notes.md", "content", "1. 拉取镜像\n2. 滚动重启\n3. 回滚"));
        runCall(pipeline, session3,
                ToolCall.of("shell", "command", "npm test"));
        runCall(pipeline, session3,
                ToolCall.of("write_file", "path", ".env", "content", "SECRET=1"));

        ctx.close();
        banner("运行时拆除完成：所有插件帧已按逆序回滚");
    }

    private static void printPromptAndTools(PluginContext ctx) {
        String prompt = ctx.service("systemPrompt", SystemPromptService.class).assemble();
        System.out.println("    系统提示: "
                + (prompt.isBlank() ? "(空——还没有插件贡献片段)" : prompt.replace("\n", " | ")));
        System.out.println("    工具清单:\n" + indent(
                ctx.service("tools", ToolPipeline.class).registry().describeAll(), "      "));
    }

    private static void printDerived(List<Message> messages) {
        for (Message message : messages) {
            System.out.println("    " + message);
        }
    }

    private static void runCall(ToolPipeline pipeline, SessionLog session, ToolCall call) {
        ToolPipeline.ToolResult result = pipeline.execute(call, session);
        System.out.printf("    -> %-12s isError=%-5s output=%s%n",
                result.tool(), result.error(), oneLine(result.output()));
    }

    private static String oneLine(String text) {
        return text.replace("\n", "\\n");
    }

    private static String indent(String text, String padding) {
        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            sb.append(padding).append(line).append('\n');
        }
        return sb.toString();
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("==================================================================");
        System.out.println("  " + title);
        System.out.println("==================================================================");
    }
}
