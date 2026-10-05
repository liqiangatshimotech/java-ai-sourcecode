package com.example.chapter09.mimocode.context;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;

/**
 * 用同步代码模拟 checkpoint-writer 子 Agent。真实 MiMo-Code 会拉起独立
 * subagent，读取主会话到 watermark 的消息，并写 checkpoint.md、MEMORY.md
 * 和任务进度。这里把写 checkpoint 的输入输出保留下来，便于观察。
 */
public final class CheckpointWriter {
    private static final int TAIL_MIN_TOKENS = 10_000;
    private static final int TAIL_MAX_TOKENS = 20_000;

    private final DemoWorkspace workspace;

    public CheckpointWriter(DemoWorkspace workspace) {
        this.workspace = workspace;
    }

    public CheckpointSnapshot write(String sessionId, List<ConversationMessage> messages, int currentTokens) throws IOException {
        int boundaryIndex = chooseTailBoundary(messages);
        List<ConversationMessage> covered = messages.subList(0, boundaryIndex);
        int coveredTokens = covered.stream().mapToInt(ConversationMessage::estimatedTokens).sum();

        StringBuilder markdown = new StringBuilder();
        markdown.append("# Session checkpoint\n\n");
        markdown.append("- session: `").append(sessionId).append("`\n");
        markdown.append("- updated_at: `").append(Instant.now()).append("`\n");
        markdown.append("- current_tokens: `").append(currentTokens).append("`\n");
        markdown.append("- recent_tail_starts_at: `").append(messages.get(boundaryIndex).id()).append("`\n\n");

        markdown.append("## Current goal\n");
        markdown.append("- Explain how MiMo-Code keeps a logical session alive across bounded model windows.\n\n");

        markdown.append("## Snapshot\n");
        markdown.append("- Messages summarized into this checkpoint: ").append(covered.size()).append("\n");
        markdown.append("- Tokens summarized into this checkpoint: ").append(coveredTokens).append("\n");
        markdown.append("- Latest decision: infinite context means checkpoint plus rebuild, not an actually infinite prompt.\n\n");

        markdown.append("## Important evidence\n");
        covered.stream()
                .filter(message -> message.text().contains("DECISION") || message.text().contains("SOURCE"))
                .forEach(message -> markdown.append(message.shortLine()).append("\n"));

        markdown.append("\n## Next step\n");
        markdown.append("- Rebuild the next request from checkpoint, project memory, notes, memory index and recent raw messages.\n");

        Files.writeString(workspace.sessionCheckpointFile(sessionId), markdown.toString());
        Files.writeString(workspace.memoryIndexFile(sessionId), """
                sessions/%s/checkpoint.md
                sessions/%s/notes.md
                projects/chapter09/MEMORY.md
                global/MEMORY.md
                """.formatted(sessionId, sessionId));

        return new CheckpointSnapshot(
                boundaryIndex,
                covered.size(),
                coveredTokens,
                workspace.sessionCheckpointFile(sessionId));
    }

    /**
     * MiMo-Code 的真实实现会保留最近一段原文 tail。这里复刻同一思路：
     * 从最后一个 assistant 前一条消息开始，如果 tail 太短就往前扩展，
     * 但扩展到约 20K token 就停，避免把过多历史重新塞进模型。
     */
    private int chooseTailBoundary(List<ConversationMessage> messages) {
        if (messages.isEmpty()) {
            return 0;
        }

        int lastAssistantIndex = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).role() == ConversationMessage.Role.ASSISTANT) {
                lastAssistantIndex = i;
                break;
            }
        }

        int start = Math.max(0, lastAssistantIndex - 1);
        int tailTokens = sumTokens(messages, start, messages.size());
        if (tailTokens >= TAIL_MAX_TOKENS) {
            return start;
        }

        while (start > 0 && tailTokens < TAIL_MIN_TOKENS) {
            start--;
            tailTokens += messages.get(start).estimatedTokens();
            if (tailTokens >= TAIL_MAX_TOKENS) {
                break;
            }
        }
        return start;
    }

    private int sumTokens(List<ConversationMessage> messages, int startInclusive, int endExclusive) {
        int sum = 0;
        for (int i = startInclusive; i < endExclusive; i++) {
            sum += messages.get(i).estimatedTokens();
        }
        return sum;
    }
}
