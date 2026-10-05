package com.example.chapter09.mimocode;

import java.io.IOException;
import java.util.List;

/**
 * Dream 和 Distill 的区别：
 * - Dream 把“事实和经验”沉淀进长期记忆；
 * - Distill 把“重复流程”沉淀成可复用技能、命令或子 Agent。
 */
public final class DreamDistillService {
    private final MarkdownMemoryStore memoryStore;

    public DreamDistillService(MarkdownMemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }

    public String dream(List<HistoryEvent> events) throws IOException {
        long memoryMentions = events.stream().filter(event -> event.body().toLowerCase().contains("memory")).count();
        long checkpointMentions = events.stream().filter(event -> event.body().toLowerCase().contains("checkpoint")).count();

        String markdown = """
                # Project memory

                ## Durable facts
                - MiMo-Code style memory should keep raw history separate from curated memory.
                - Checkpoint is a session continuity artifact, not a full transcript.
                - Dream should promote only facts that appear in raw history or repeated work.

                ## Repeated signals from this run
                - memory mentions: %d
                - checkpoint mentions: %d

                ## Coding convention
                - Java examples should keep DeepSeek as the default provider while leaving Ollama and ChatGPT configurable.
                """.formatted(memoryMentions, checkpointMentions);
        memoryStore.writeProjectMemory(markdown);
        return markdown;
    }

    public String distill(List<HistoryEvent> events) throws IOException {
        String markdown = """
                # Distilled workflow

                ## Name
                MiMo-style memory page authoring workflow

                ## Trigger
                Use this when a Coding Agent architecture page discusses long-term memory, checkpoint, raw history, or self-improving workflows.

                ## Steps
                1. Read the raw trajectory or source code before writing memory claims.
                2. Write project facts into project memory only after evidence is found.
                3. Create a checkpoint when the task needs to survive context pruning.
                4. Link every theory section to a runnable Java class.
                5. Add one diagram for each major state transition or feedback loop.

                ## Why this belongs in Distill
                The same documentation pattern appears across memory, checkpoint, history, Dream/Distill, and workflow sections.
                """;
        memoryStore.writeTaskProgress("S-095", "T-distill", markdown);
        return markdown;
    }
}
