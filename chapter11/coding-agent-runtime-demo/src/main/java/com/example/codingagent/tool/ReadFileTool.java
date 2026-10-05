package com.example.codingagent.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

/**
 * 读取文件时必须经过 WorkspaceBoundary，防止模型构造 ../ 读取仓库外文件。
 */
public final class ReadFileTool implements AgentTool {

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, String> arguments) throws IOException {
        String path = arguments.get("path");
        String content = Files.readString(context.boundary().resolve(path));
        return ToolResult.success("读取 " + path, content);
    }
}
