package com.example.chapter09.dsh.core;

import com.example.chapter09.dsh.plugin.PluginContext;
import com.example.chapter09.dsh.tools.ToolDefinition;
import com.example.chapter09.dsh.tools.ToolPipeline;

/**
 * 可卸载的“团队规范”插件：演示注册的完全可逆性。
 *
 * 它挂上来时贡献三样东西：一段系统提示词、一个 java_version 工具、
 * 一个 tools/result 遥测监听。卸载后三样东西同时消失，
 * 运行时回到它从未来过的状态——这就是 Cordis 论文里
 * “时间可组合性：组件移除时副作用完全撤销”的工程含义。
 */
public final class GuidelinePlugin {

    public static void load(PluginContext ctx) {
        ctx.load("team-guideline", c -> {
            // 1) 提示词片段
            SystemPromptService prompt = c.service("systemPrompt", SystemPromptService.class);
            c.effect(prompt.addFragment("guideline",
                    "Follow repo conventions. Prefer small, reviewable changes."));

            // 2) 一个只读工具
            ToolPipeline pipeline = c.service("tools", ToolPipeline.class);
            pipeline.registerTool(new ToolDefinition(
                    "java_version",
                    "查看当前 JDK 版本",
                    java.util.Map.of(),
                    (call, cc) -> "Java " + System.getProperty("java.version")));

            // 3) 遥测监听（on 会自动登记退订 disposer）
            c.on("tools/result", args ->
                    System.out.println("    [telemetry] tool settled: " + args[0]));
        });
    }
}
