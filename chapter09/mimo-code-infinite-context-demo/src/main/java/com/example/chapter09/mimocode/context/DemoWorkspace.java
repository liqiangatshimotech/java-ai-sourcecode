package com.example.chapter09.mimocode.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件布局模拟 MiMo-Code 的 memory 目录。真实项目里这些文件位于
 * data/memory/sessions、data/memory/projects 和 data/memory/global 下。
 */
public final class DemoWorkspace {
    private final Path root;

    public DemoWorkspace(Path root) {
        this.root = root;
    }

    public void prepare(String sessionId) throws IOException {
        Files.createDirectories(sessionDir(sessionId));
        Files.createDirectories(projectDir());
        Files.createDirectories(globalDir());

        Files.writeString(projectMemoryFile(), """
                # Project memory

                ## Stable facts
                - MiMo-Code 的无限上下文不是单次模型调用无限，而是有界窗口的连续重建。
                - checkpoint.md 保存当前会话状态，MEMORY.md 保存跨会话项目事实。
                - Java 示例默认以 DeepSeek 为 provider，同时保留 Ollama 和 ChatGPT 配置。
                """);

        Files.writeString(globalMemoryFile(), """
                # Global memory

                ## Writing preference
                - 技术文档要先给结论，再拆源码和工程边界。
                """);

        Files.writeString(notesFile(sessionId), """
                # Session notes

                - 这次重点讲清楚 cycle、checkpoint writer、rebuild context 和 microcompact。
                """);
    }

    public Path sessionCheckpointFile(String sessionId) {
        return sessionDir(sessionId).resolve("checkpoint.md");
    }

    public Path notesFile(String sessionId) {
        return sessionDir(sessionId).resolve("notes.md");
    }

    public Path projectMemoryFile() {
        return projectDir().resolve("MEMORY.md");
    }

    public Path globalMemoryFile() {
        return globalDir().resolve("MEMORY.md");
    }

    public Path memoryIndexFile(String sessionId) {
        return sessionDir(sessionId).resolve("memory-keys.txt");
    }

    private Path sessionDir(String sessionId) {
        return root.resolve("memory").resolve("sessions").resolve(sessionId);
    }

    private Path projectDir() {
        return root.resolve("memory").resolve("projects").resolve("chapter09");
    }

    private Path globalDir() {
        return root.resolve("memory").resolve("global");
    }
}
