package com.example.codingagent.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * 记录运行前的文本文件快照，用于最后生成 diff。
 */
public record WorkspaceSnapshot(Map<String, String> files) {

    public static WorkspaceSnapshot capture(Path root) throws IOException {
        WorkspaceBoundary boundary = new WorkspaceBoundary(root);
        boundary.resolve(".");
        Map<String, String> files = new TreeMap<>();
        try (var stream = Files.walk(root)) {
            for (Path path : stream.toList()) {
                String relative = toWorkspacePath(root.relativize(path));
                boundary.resolve(relative.isBlank() ? "." : relative);
                if (!Files.isRegularFile(path)) continue;
                if (isTextFile(path)) {
                    if (Files.size(path) > 250_000 || files.size() >= 20) {
                        throw new IOException("演示工作区超出文件大小或数量预算");
                    }
                    files.put(relative, Files.readString(path));
                }
            }
        }
        return new WorkspaceSnapshot(Map.copyOf(files));
    }

    /**
     * 把相对路径统一成 "/" 分隔的工作区路径。
     * Windows 上 Path.toString() 得到的是 src\main\java\...，Linux / macOS 上是 src/main/java/...；
     * 快照 key、工具输出和 CodingAgentRuntime.TARGET_PATH 必须用同一种写法，否则在 Windows 上会找不到计费源文件。
     */
    public static String toWorkspacePath(Path relative) {
        StringBuilder builder = new StringBuilder();
        for (Path part : relative) {
            if (builder.length() > 0) builder.append('/');
            builder.append(part);
        }
        return builder.toString();
    }

    private static boolean isTextFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".java") || name.endsWith(".md") || name.endsWith(".xml")
                || name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".properties");
    }
}
