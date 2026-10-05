package com.example.codingagent.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 写文件是高风险工具，Runtime 会强制要求审批通过后才能调用。
 */
public final class WriteFileTool implements AgentTool {

    @Override
    public String name() {
        return "write_file";
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.HIGH;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, String> arguments) throws IOException {
        String path = arguments.get("path");
        String content = arguments.getOrDefault("content", "");
        Path target = context.boundary().resolve(path);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
        return ToolResult.success("写入 " + path, "bytes=" + content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
    }
}
