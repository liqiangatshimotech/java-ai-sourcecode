package com.example.chapter09.mimocode.context;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * 重建下一轮上下文。它不是简单 summary，而是按优先级拼接多个来源：
 * checkpoint、project/global memory、session notes、memory keys index，
 * 最后再接近期原始消息。这个顺序和 MiMo-Code 源码里的 renderRebuildContext
 * 思路一致。
 */
public final class RebuildContextBuilder {
    private final DemoWorkspace workspace;

    public RebuildContextBuilder(DemoWorkspace workspace) {
        this.workspace = workspace;
    }

    public String build(String sessionId, CheckpointSnapshot snapshot, List<ConversationMessage> messages) throws IOException {
        StringBuilder context = new StringBuilder();
        context.append("The following blocks are auto-loaded from session memory.\n\n");

        context.append("## Tasks ledger\n");
        context.append("- T-9.6 in_progress -- explain MiMo-Code infinite context with source-backed Java demo.\n\n");

        context.append("## Session checkpoint\n");
        context.append(Files.readString(snapshot.checkpointFile())).append("\n");

        context.append("## Project memory\n");
        context.append(Files.readString(workspace.projectMemoryFile())).append("\n");

        context.append("## Global memory\n");
        context.append(Files.readString(workspace.globalMemoryFile())).append("\n");

        context.append("## Session notes\n");
        context.append(Files.readString(workspace.notesFile(sessionId))).append("\n");

        context.append("## Memory keys index\n");
        context.append(Files.readString(workspace.memoryIndexFile(sessionId))).append("\n");

        context.append("## Recent messages preserved verbatim\n");
        for (int i = snapshot.boundaryIndex(); i < messages.size(); i++) {
            context.append(messages.get(i).shortLine()).append("\n");
        }

        context.append("\nResume directly. Do not restart the task or ask the user to repeat the goal.\n");
        return context.toString();
    }
}
