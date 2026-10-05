package com.example.chapter09.mimocode.context;

import java.nio.file.Path;

/**
 * 运行这个 main 方法可以看到“无限上下文”的核心过程。
 *
 * 这里没有调用真实模型，因为真正要讲的是 runtime 如何组织上下文：
 * 大模型仍然只接收一次有限窗口请求；无限感来自 checkpoint 文件、
 * memory 文件、近期原文 tail 和下一轮上下文重建。
 */
public final class Chapter096Demo {
    private static final String SESSION_ID = "S-096";

    private Chapter096Demo() {
    }

    public static void main(String[] args) throws Exception {
        DemoWorkspace workspace = new DemoWorkspace(Path.of("target", "mimo-code-infinite-context-demo"));
        workspace.prepare(SESSION_ID);

        // 这些 history / SOURCE 文本是内置机制演示夹具，不代表读取或审计过外部仓库。
        System.out.println("DEMO_MODE=SIMULATED; external source scan=NOT_PERFORMED; model calls=NONE");

        AiProviderConfig provider = AiProviderConfig.fromEnvironment(System.getenv());
        ModelWindow window = new ModelWindow(128_000, 16_000, 20_000);
        InfiniteContextRuntime runtime = new InfiniteContextRuntime(SESSION_ID, window, workspace);

        printHeader(provider, window, runtime);
        simulateLongTask(runtime);
        printRebuild(runtime);
    }

    private static void printHeader(AiProviderConfig provider, ModelWindow window, InfiniteContextRuntime runtime) {
        System.out.println("== Provider profile ==");
        System.out.println("provider = " + provider.provider());
        System.out.println("model    = " + provider.model());
        System.out.println("baseUrl  = " + provider.baseUrl());
        System.out.println("apiKey   = " + provider.maskedApiKey());
        System.out.println();

        System.out.println("== Context budget ==");
        System.out.printf("physical context = %,d tokens%n", window.contextTokens());
        System.out.printf("usable input     = %,d tokens%n", window.usableInputTokens());
        System.out.println("thresholds       = " + runtime.thresholds());
        System.out.println();
    }

    private static void simulateLongTask(InfiniteContextRuntime runtime) throws Exception {
        System.out.println("== Simulate a long coding session ==");
        append(runtime, ConversationMessage.user("m1", "Need to update chapter 9.6 and explain MiMo-Code infinite context.", 3_500));
        append(runtime, ConversationMessage.assistant("m2", "Plan: read 9.5 style, inspect MiMo-Code source, then write source-backed explanation.", 4_800));
        append(runtime, ConversationMessage.toolResult("m3", "read", "SOURCE packages/opencode/src/session/prune.ts shows threshold density by window size.", 13_500, true));
        append(runtime, ConversationMessage.assistant("m4", "DECISION: explain infinite context as bounded cycles, not a truly unlimited model prompt.", 6_800));
        append(runtime, ConversationMessage.toolResult("m5", "read", "SOURCE packages/opencode/src/session/checkpoint.ts shows checkpoint writer and rebuild context.", 15_000, true));
        append(runtime, ConversationMessage.user("m6", "Keep the wording like a technical sharing note, not a generic product intro.", 3_200));
        append(runtime, ConversationMessage.assistant("m7", "Draft structure: conclusion, cycle diagram, source snippets, Java mapping, operational risks.", 7_200));
        append(runtime, ConversationMessage.toolResult("m8", "bash", "Large test log from Java demo compile and run. It is useful once, but repeatable.", 12_400, true));
        append(runtime, ConversationMessage.assistant("m9", "DECISION: preserve recent user intent verbatim so rebuild does not drift from the original request.", 7_600));
        append(runtime, ConversationMessage.toolResult("m10", "grep", "SOURCE memory/service.ts shows SQLite FTS5 search over memory files.", 10_500, true));
        append(runtime, ConversationMessage.assistant("m11", "Ready to rebuild context from checkpoint, memory, notes, index, and recent tail.", 7_300));
    }

    private static void append(InfiniteContextRuntime runtime, ConversationMessage message) throws Exception {
        runtime.append(message);
        System.out.printf("append %-3s total=%,d tokens%n", message.id(), runtime.totalTokens());
    }

    private static void printRebuild(InfiniteContextRuntime runtime) throws Exception {
        System.out.println();
        System.out.println("== Rebuild context ==");
        String rebuilt = runtime.rebuildIfNeeded().orElse("(not enough pressure to rebuild)");
        System.out.println(firstLines(rebuilt, 46));
        System.out.println();
        System.out.println("checkpoint file = " + runtime.latestCheckpoint().checkpointFile());
    }

    private static String firstLines(String text, int lines) {
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (String line : text.split("\\R")) {
            out.append(line).append(System.lineSeparator());
            count++;
            if (count >= lines) {
                out.append("...").append(System.lineSeparator());
                break;
            }
        }
        return out.toString();
    }
}
