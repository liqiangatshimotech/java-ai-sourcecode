package com.example.chapter09.mimocode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 运行这个 main 方法，可以看到 9.5 里每个理论点在 Java 里的最小落地形态。
 * 它不是完整 Agent 框架，而是把 MiMo-Code 的关键机制拆成容易观察的步骤。
 */
public final class Chapter095Demo {
    private static final String SESSION_ID = "S-095";

    private Chapter095Demo() {
    }

    public static void main(String[] args) throws Exception {
        DemoWorkspace workspace = new DemoWorkspace(Path.of("target", "mimo-code-memory-demo"));
        workspace.prepare();

        // 这些 history / SOURCE 文本是内置机制演示夹具，不代表读取或审计过外部仓库。
        System.out.println("DEMO_MODE=SIMULATED; external source scan=NOT_PERFORMED; model calls=NONE");

        AiProviderConfig provider = AiProviderConfig.fromEnvironment(System.getenv());
        HistoryStore history = new HistoryStore(workspace);
        MarkdownMemoryStore memoryStore = new MarkdownMemoryStore(workspace);
        TaskBoard taskBoard = new TaskBoard();
        ActorRegistry actorRegistry = new ActorRegistry();

        printProvider(provider);
        simulateWork(history, taskBoard, memoryStore);
        runActorWorkflow(actorRegistry);
        writeCheckpoint(history, memoryStore);
        runDreamAndDistill(history, memoryStore);
        runMemorySearch(memoryStore);
        runWriteGuard(workspace);
        printArtifacts(workspace);
    }

    private static void printProvider(AiProviderConfig provider) {
        System.out.println("== Provider profile ==");
        System.out.println("provider = " + provider.provider());
        System.out.println("model    = " + provider.model());
        System.out.println("baseUrl  = " + provider.baseUrl());
        System.out.println("apiKey   = " + provider.maskedApiKey());
        System.out.println();
    }

    private static void simulateWork(HistoryStore history, TaskBoard taskBoard, MarkdownMemoryStore memoryStore) throws IOException {
        System.out.println("== Simulate raw history and task progress ==");
        taskBoard.create("T-9.5", "Expand MiMo-Code memory docs and connect them to Java examples.");
        taskBoard.markInProgress("T-9.5");

        history.append(SESSION_ID, HistoryEvent.EventKind.USER_MESSAGE,
                "Need richer docs",
                "9.5 pages are too thin; add Java examples, workflow diagrams, and business diagrams.");
        history.append(SESSION_ID, HistoryEvent.EventKind.ASSISTANT_MESSAGE,
                "Plan",
                "Create a Java demo for memory, checkpoint, history, Dream, Distill, and task progress.");
        history.append(SESSION_ID, HistoryEvent.EventKind.TOOL_CALL,
                "Create module",
                "Write mimo-code-memory-demo under chapter09.");
        history.append(SESSION_ID, HistoryEvent.EventKind.TOOL_RESULT,
                "Module created",
                "Java module includes memory store, full-text index, checkpoint writer, history store, and distill workflow.");

        taskBoard.markDone("T-9.5", "Demo module created; documents should link each theory page to one Java class.");
        memoryStore.writeTaskProgress(SESSION_ID, "T-9.5", """
                # Task progress

                ## Status
                DONE

                ## Result
                Java demo module created for MiMo-Code memory mechanics.

                ## Files worth reading
                - Chapter095Demo.java
                - MarkdownMemoryStore.java
                - FullTextMemoryIndex.java
                - CheckpointWriter.java
                - DreamDistillService.java
                """);
        System.out.println("events = " + history.all().size());
        System.out.println("tasks  = " + taskBoard.tasks().size());
        System.out.println();
    }

    private static void runActorWorkflow(ActorRegistry actorRegistry) {
        System.out.println("== Run task, actor, and workflow ==");
        SimpleWorkflowRuntime runtime = new SimpleWorkflowRuntime(actorRegistry);
        SimpleWorkflowRuntime.WorkflowRun run = runtime.run(
                "expand-9.5-docs",
                SESSION_ID,
                "T-9.5",
                List.of(
                        SimpleWorkflowRuntime.WorkflowStep.local("Collect MiMo-Code source references"),
                        SimpleWorkflowRuntime.WorkflowStep.agent(
                                "Analyze memory source",
                                "source-analyzer",
                                "Read MiMo-Code memory and checkpoint source files."),
                        SimpleWorkflowRuntime.WorkflowStep.agent(
                                "Write Java comparison",
                                "java-example-writer",
                                "Map TypeScript mechanisms to the Java demo classes.")));

        System.out.println("workflow = " + run.id() + " / " + run.name());
        System.out.println("actors   = " + actorRegistry.actors().size());
        System.out.println("journal  = " + runtime.journal().size());
        System.out.println();
    }

    private static void writeCheckpoint(HistoryStore history, MarkdownMemoryStore memoryStore) throws IOException {
        System.out.println("== Write checkpoint ==");
        CheckpointWriter checkpointWriter = new CheckpointWriter(memoryStore);
        String checkpoint = checkpointWriter.write(SESSION_ID, history.all());
        history.append(SESSION_ID, HistoryEvent.EventKind.CHECKPOINT, "Checkpoint written", checkpoint);
        System.out.println(firstLine(checkpoint));
        System.out.println();
    }

    private static void runDreamAndDistill(HistoryStore history, MarkdownMemoryStore memoryStore) throws IOException {
        System.out.println("== Dream and Distill ==");
        DreamDistillService service = new DreamDistillService(memoryStore);
        String memory = service.dream(history.all());
        String workflow = service.distill(history.all());
        history.append(SESSION_ID, HistoryEvent.EventKind.DREAM, "Project memory updated", memory);
        history.append(SESSION_ID, HistoryEvent.EventKind.DISTILL, "Workflow distilled", workflow);
        System.out.println(firstLine(memory));
        System.out.println(firstLine(workflow));
        System.out.println();
    }

    private static void runMemorySearch(MarkdownMemoryStore memoryStore) throws IOException {
        System.out.println("== Search memory index ==");
        FullTextMemoryIndex index = memoryStore.rebuildIndex();
        List<FullTextMemoryIndex.SearchHit> hits = index.search("checkpoint memory", 5);
        for (FullTextMemoryIndex.SearchHit hit : hits) {
            System.out.printf("- score=%.2f scope=%s type=%s path=%s%n  %s%n",
                    hit.score(),
                    hit.document().scope(),
                    hit.document().type(),
                    hit.document().path().getFileName(),
                    hit.snippet());
        }
        System.out.println();
    }

    private static void runWriteGuard(DemoWorkspace workspace) {
        System.out.println("== Check write guard ==");
        MemoryPathGuard guard = new MemoryPathGuard(workspace);
        System.out.println("checkpoint writer -> checkpoint.md = "
                + guard.canWrite(MemoryPathGuard.WriterRole.CHECKPOINT_WRITER, workspace.sessionCheckpointFile(SESSION_ID)));
        System.out.println("task agent -> MEMORY.md = "
                + guard.canWrite(MemoryPathGuard.WriterRole.TASK_AGENT, workspace.projectMemoryFile()));
        System.out.println();
    }

    private static void printArtifacts(DemoWorkspace workspace) {
        System.out.println("== Generated artifacts ==");
        System.out.println(workspace.historyFile());
        System.out.println(workspace.projectMemoryFile());
        System.out.println(workspace.sessionCheckpointFile(SESSION_ID));
        System.out.println(workspace.taskProgressFile(SESSION_ID, "T-9.5"));
    }

    private static String firstLine(String text) {
        return text.lines().findFirst().orElse("");
    }
}
