package com.example.codingagent.tool;

import com.example.codingagent.workspace.DiffService;

import java.io.IOException;
import java.util.Map;

/**
 * 示例里不依赖真实 git 命令，而是用运行前快照和当前文件生成 diff。
 * 好处是测试稳定，也能说明 Coding Agent 交付前必须展示改动范围。
 */
public final class GitDiffTool implements AgentTool {

    private final DiffService diffService = new DiffService();

    @Override
    public String name() {
        return "git_diff";
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, String> arguments) throws IOException {
        String diff = diffService.diff(context.baseline(), context.workspaceRoot());
        return ToolResult.success("生成 diff", diff);
    }
}
