package com.example.codingagent.workspace;

import java.nio.file.Path;

/**
 * 工作区边界检查。
 * 任何文件工具都必须先 resolve 目标路径，确保最终路径仍在 workspaceRoot 之内。
 */
public final class WorkspaceBoundary {

    private final Path workspaceRoot;

    public WorkspaceBoundary(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
    }

    public Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath must not be blank");
        }
        if (Path.of(relativePath).isAbsolute()) throw new SecurityException("Absolute paths are not allowed");
        Path resolved = workspaceRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(workspaceRoot)) {
            throw new SecurityException("Path escapes workspace: " + relativePath);
        }
        // normalize 不能识别符号链接：逐段拒绝链接，防止指向工作区外。
        Path current = workspaceRoot;
        if (java.nio.file.Files.isSymbolicLink(current)) throw new SecurityException("Symbolic workspace root");
        for (Path part : workspaceRoot.relativize(resolved)) {
            current = current.resolve(part);
            if (java.nio.file.Files.isSymbolicLink(current)) throw new SecurityException("Symbolic paths are not allowed");
        }
        return resolved;
    }
}
