package com.example.chapter09.mimocode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * demo 的所有运行产物都放在 target 目录下，避免污染源码目录。
 * 真正的 Coding Agent 可以把这里换成用户级 data 目录，例如
 * ~/.local/share/xxx/memory。
 */
public final class DemoWorkspace {
    private final Path root;

    public DemoWorkspace(Path root) {
        this.root = root;
    }

    public Path root() {
        return root;
    }

    public Path memoryRoot() {
        return root.resolve("memory");
    }

    public Path historyFile() {
        return root.resolve("history").resolve("events.log");
    }

    public Path projectMemoryFile() {
        return memoryRoot().resolve("projects").resolve("chapter09").resolve("MEMORY.md");
    }

    public Path globalMemoryFile() {
        return memoryRoot().resolve("global").resolve("MEMORY.md");
    }

    public Path sessionCheckpointFile(String sessionId) {
        return memoryRoot().resolve("sessions").resolve(sessionId).resolve("checkpoint.md");
    }

    public Path taskProgressFile(String sessionId, String taskId) {
        return memoryRoot().resolve("sessions").resolve(sessionId).resolve("tasks").resolve(taskId).resolve("progress.md");
    }

    public void prepare() throws IOException {
        Files.createDirectories(root);
        Files.createDirectories(memoryRoot());
        Files.createDirectories(historyFile().getParent());
        Files.createDirectories(projectMemoryFile().getParent());
        Files.createDirectories(globalMemoryFile().getParent());
    }
}
