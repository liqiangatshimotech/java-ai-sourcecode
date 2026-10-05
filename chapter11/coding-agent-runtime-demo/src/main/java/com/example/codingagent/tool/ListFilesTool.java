package com.example.codingagent.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 列出工作区中的源文件。
 * Coding Agent 不应该一上来全量读取所有文件，先列目录能降低上下文成本。
 */
public final class ListFilesTool implements AgentTool {

    @Override
    public String name() {
        return "list_files";
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, String> arguments) throws IOException {
        try (var stream = Files.walk(context.workspaceRoot())) {
            String output = stream
                    .filter(Files::isRegularFile)
                    .map(context.workspaceRoot()::relativize)
                    .map(Path::toString)
                    .filter(path -> !path.contains(".git"))
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.joining(System.lineSeparator()));
            return ToolResult.success("列出工作区文件", output);
        }
    }
}
