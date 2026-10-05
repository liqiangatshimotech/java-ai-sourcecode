package com.example.chapter09.mimocode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 用 Markdown 文件保存长期记忆。这样做的好处是容易审阅、容易迁移，
 * 也方便在调试时直接打开文件看 Agent 到底记住了什么。
 */
public final class MarkdownMemoryStore {
    private final DemoWorkspace workspace;

    public MarkdownMemoryStore(DemoWorkspace workspace) {
        this.workspace = workspace;
    }

    public void writeProjectMemory(String body) throws IOException {
        write(workspace.projectMemoryFile(), body);
    }

    public void writeGlobalMemory(String body) throws IOException {
        write(workspace.globalMemoryFile(), body);
    }

    public void writeSessionCheckpoint(String sessionId, String body) throws IOException {
        write(workspace.sessionCheckpointFile(sessionId), body);
    }

    public void writeTaskProgress(String sessionId, String taskId, String body) throws IOException {
        write(workspace.taskProgressFile(sessionId, taskId), body);
    }

    public List<MemoryDocument> scan() throws IOException {
        List<MemoryDocument> documents = new ArrayList<>();
        if (!Files.exists(workspace.memoryRoot())) {
            return documents;
        }
        try (var paths = Files.walk(workspace.memoryRoot())) {
            for (Path path : paths.filter(path -> path.toString().endsWith(".md")).toList()) {
                String relative = workspace.memoryRoot().relativize(path).toString();
                String[] parts = relative.split(java.io.File.separator.equals("\\") ? "\\\\" : java.io.File.separator);
                String scope = parts.length > 0 ? parts[0] : "unknown";
                String type = path.getFileName().toString().replace(".md", "");
                documents.add(new MemoryDocument(relative, scope, type, path, Files.readString(path, StandardCharsets.UTF_8)));
            }
        }
        return documents;
    }

    public FullTextMemoryIndex rebuildIndex() throws IOException {
        FullTextMemoryIndex index = new FullTextMemoryIndex();
        for (MemoryDocument document : scan()) {
            index.add(document);
        }
        return index;
    }

    private static void write(Path path, String body) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, body, StandardCharsets.UTF_8);
    }
}
