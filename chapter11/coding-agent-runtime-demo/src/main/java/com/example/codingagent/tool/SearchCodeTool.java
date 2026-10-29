package com.example.codingagent.tool;

import com.example.codingagent.workspace.WorkspaceSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 简化版代码搜索工具。
 * 只搜索常见文本文件，输出格式为 path:line:content，方便 Runtime 再去 read_file 精读。
 */
public final class SearchCodeTool implements AgentTool {

    @Override
    public String name() {
        return "search_code";
    }

    @Override
    public ToolRisk risk() {
        return ToolRisk.LOW;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, String> arguments) throws IOException {
        String query = arguments.getOrDefault("query", "");
        if (query.isBlank()) {
            return ToolResult.failure("搜索失败", "query 不能为空");
        }

        try (var stream = Files.walk(context.workspaceRoot())) {
            String output = stream
                    .filter(Files::isRegularFile)
                    .filter(this::isSearchable)
                    .map(path -> context.boundary().resolve(WorkspaceSnapshot.toWorkspacePath(context.workspaceRoot().relativize(path))))
                    .flatMap(path -> searchFile(context.workspaceRoot(), path, query).stream())
                    .collect(Collectors.joining(System.lineSeparator()));
            if (output.isBlank()) {
                return ToolResult.failure("没有命中", "query=" + query);
            }
            return ToolResult.success("搜索命中", output);
        }
    }

    private java.util.List<String> searchFile(Path root, Path path, String query) {
        try {
            java.util.List<String> lines = Files.readAllLines(path);
            java.util.List<String> hits = new java.util.ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains(query)) {
                    // 命中行格式：相对路径:行号:内容，路径统一用 "/"，Runtime 会用它和 TARGET_PATH 比对
                    hits.add(WorkspaceSnapshot.toWorkspacePath(root.relativize(path)) + ":" + (i + 1) + ":" + lines.get(i).trim());
                }
            }
            return hits;
        } catch (IOException ignored) {
            return java.util.List.of();
        }
    }

    private boolean isSearchable(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".java") || name.endsWith(".md") || name.endsWith(".yml")
                || name.endsWith(".yaml") || name.endsWith(".properties");
    }
}
