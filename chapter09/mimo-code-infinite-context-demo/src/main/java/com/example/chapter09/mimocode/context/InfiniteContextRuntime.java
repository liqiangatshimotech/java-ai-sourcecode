package com.example.chapter09.mimocode.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * 最小版无限上下文 runtime。重点看三个动作：
 *
 * 1. fireCheckpoints：token 经过阈值时写 checkpoint。
 * 2. rebuildIfNeeded：达到最大阈值后插入重建上下文，开启新 cycle。
 * 3. microCompactTail：对可重放的大工具结果只保留占位，避免下一次请求被日志撑爆。
 */
public final class InfiniteContextRuntime {
    private final String sessionId;
    private final ModelWindow window;
    private final List<Integer> thresholds;
    private final CheckpointWriter checkpointWriter;
    private final RebuildContextBuilder rebuildContextBuilder;
    private final List<ConversationMessage> messages = new ArrayList<>();
    private final TreeSet<Integer> crossedThresholds = new TreeSet<>();
    private CheckpointSnapshot latestCheckpoint;

    public InfiniteContextRuntime(String sessionId, ModelWindow window, DemoWorkspace workspace) {
        this.sessionId = sessionId;
        this.window = window;
        this.thresholds = CheckpointThresholdPolicy.defaultThresholdsFor(window.usableInputTokens());
        this.checkpointWriter = new CheckpointWriter(workspace);
        this.rebuildContextBuilder = new RebuildContextBuilder(workspace);
    }

    public void append(ConversationMessage message) throws IOException {
        messages.add(message);
        fireCheckpoints();
    }

    public Optional<String> rebuildIfNeeded() throws IOException {
        if (latestCheckpoint == null || crossedThresholds.isEmpty()) {
            return Optional.empty();
        }
        int maxThreshold = thresholds.get(thresholds.size() - 1);
        if (!crossedThresholds.contains(maxThreshold) && totalTokens() < window.usableInputTokens()) {
            return Optional.empty();
        }

        microCompactTail();
        String rebuilt = rebuildContextBuilder.build(sessionId, latestCheckpoint, messages);
        return Optional.of(rebuilt);
    }

    public int totalTokens() {
        return messages.stream().mapToInt(ConversationMessage::estimatedTokens).sum();
    }

    public List<Integer> thresholds() {
        return thresholds;
    }

    public CheckpointSnapshot latestCheckpoint() {
        return latestCheckpoint;
    }

    private void fireCheckpoints() throws IOException {
        if (thresholds.isEmpty()) {
            return;
        }
        int currentTokens = totalTokens();
        for (int threshold : thresholds) {
            if (currentTokens < threshold || crossedThresholds.contains(threshold)) {
                continue;
            }
            latestCheckpoint = checkpointWriter.write(sessionId, messages, currentTokens);
            crossedThresholds.add(threshold);
            System.out.printf("checkpoint triggered at %,d tokens, threshold %,d, tail starts at message #%d%n",
                    currentTokens,
                    threshold,
                    latestCheckpoint.boundaryIndex());
        }
    }

    private void microCompactTail() {
        for (int i = latestCheckpoint.boundaryIndex(); i < messages.size(); i++) {
            ConversationMessage message = messages.get(i);
            if (message.compactableToolResult()) {
                messages.set(i, message.compactedCopy());
            }
        }
    }
}
