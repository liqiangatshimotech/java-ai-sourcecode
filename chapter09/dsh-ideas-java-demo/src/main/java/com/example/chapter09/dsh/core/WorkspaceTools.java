package com.example.chapter09.dsh.core;

import com.example.chapter09.dsh.plugin.PluginContext;
import com.example.chapter09.dsh.tools.ToolCall;
import com.example.chapter09.dsh.tools.ToolDefinition;
import com.example.chapter09.dsh.tools.ToolPipeline;

import java.nio.file.Path;
import java.util.Map;

/**
 * 面向模型的基础工具：write_file / read_file / shell。
 *
 * 这些工具是 seam 的 Consumer——它们只认 ctx.fs 和审批策略，
 * 不 import 任何具体 Provider。第三幕把 fs 从 local 换成 sandbox 时，
 * 这个类的字节码一行都没变。
 */
public final class WorkspaceTools {

    private WorkspaceTools() {
    }

    public static void register(PluginContext ctx, Path workspaceRoot) {
        ToolPipeline pipeline = ctx.service("tools", ToolPipeline.class);

        pipeline.registerTool(new ToolDefinition(
                "write_file",
                "把内容写入工作区相对路径的文件",
                Map.of("path", "相对工作区根目录的文件路径", "content", "要写入的文本内容"),
                (call, c) -> {
                    FileSystemTarget target = resolve(call, workspaceRoot);
                    // fs/write-intent 事件在写入前发出：观测插件可以在这里做
                    // “先读后写”等检查（dsh 的 fs-observation-policy 思路）。
                    c.bus().emit("fs/write-intent", target.resolved());
                    c.service("fs", com.example.chapter09.dsh.seam.FileSystemSeam.class)
                            .write(target.resolved(), call.args().get("content"));
                    return "wrote " + call.args().get("path") + " via "
                            + c.service("fs", com.example.chapter09.dsh.seam.FileSystemSeam.class)
                                    .providerName();
                }));

        pipeline.registerTool(new ToolDefinition(
                "read_file",
                "读取工作区相对路径的文件内容",
                Map.of("path", "相对工作区根目录的文件路径"),
                (call, c) -> c.service("fs", com.example.chapter09.dsh.seam.FileSystemSeam.class)
                        .read(resolve(call, workspaceRoot).resolved())));

        pipeline.registerTool(new ToolDefinition(
                "shell",
                "在沙箱里执行一条 shell 命令（模拟）",
                Map.of("command", "要执行的命令行"),
                (call, c) -> {
                    // 真实 dsh 里这里是 ctx.shell（bash-local / bash-sandbox / pwsh-local），
                    // 走 subprocess seam；demo 模拟成回显，重点演示审批流程。
                    return "$(sandbox) " + call.args().get("command");
                }));
    }

    /** 把工具参数里的相对路径解析到本次演示的工作区根。 */
    private static FileSystemTarget resolve(ToolCall call, Path workspaceRoot) {
        String raw = call.args().getOrDefault("path", "");
        return new FileSystemTarget(workspaceRoot.resolve(raw).normalize());
    }

    private record FileSystemTarget(Path resolved) {
    }
}
