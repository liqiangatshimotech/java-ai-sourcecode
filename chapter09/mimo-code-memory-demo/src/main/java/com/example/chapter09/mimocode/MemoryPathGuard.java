package com.example.chapter09.mimocode;

import java.nio.file.Path;

/**
 * 写入边界是长期记忆系统里很容易被忽视的一点。如果所有 Agent 都能随便
 * 修改 MEMORY.md，错误记忆和越权写入都会变得很难追踪。
 */
public final class MemoryPathGuard {
    private final DemoWorkspace workspace;

    public MemoryPathGuard(DemoWorkspace workspace) {
        this.workspace = workspace;
    }

    public boolean canWrite(WriterRole role, Path target) {
        Path normalized = target.toAbsolutePath().normalize();
        Path memoryRoot = workspace.memoryRoot().toAbsolutePath().normalize();
        if (!normalized.startsWith(memoryRoot)) {
            return false;
        }

        return switch (role) {
            case CHECKPOINT_WRITER -> normalized.endsWith("checkpoint.md") || normalized.endsWith("MEMORY.md");
            case TASK_AGENT -> normalized.endsWith("progress.md");
            case MAIN_AGENT -> normalized.endsWith("MEMORY.md");
        };
    }

    public enum WriterRole {
        MAIN_AGENT,
        CHECKPOINT_WRITER,
        TASK_AGENT
    }
}
