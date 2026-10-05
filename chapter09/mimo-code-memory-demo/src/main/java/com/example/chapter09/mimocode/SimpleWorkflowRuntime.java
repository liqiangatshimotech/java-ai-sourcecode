package com.example.chapter09.mimocode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 极简 workflow 运行器。真实 MiMo-Code 使用 QuickJS 沙箱、并发信号量和
 * 持久化 journal；这个版本只保留最容易理解的一层：把流程拆成步骤，
 * 每个步骤可以拉起一个子 Agent，然后把执行轨迹写进 journal。
 */
public final class SimpleWorkflowRuntime {
    private final ActorRegistry actorRegistry;
    private final List<JournalEntry> journal = new ArrayList<>();
    private int runSequence = 1;

    public SimpleWorkflowRuntime(ActorRegistry actorRegistry) {
        this.actorRegistry = actorRegistry;
    }

    public WorkflowRun run(String workflowName, String parentSessionId, String taskId, List<WorkflowStep> steps) {
        String runId = "W-" + runSequence++;
        append(runId, "START", workflowName);

        for (WorkflowStep step : steps) {
            append(runId, "STEP_START", step.name());
            if (step.spawnSubAgent()) {
                ActorRegistry.ActorRecord actor = actorRegistry.spawnSubAgent(
                        parentSessionId,
                        taskId,
                        step.agentType(),
                        step.instruction());
                actorRegistry.markDone(actor.id(), "Finished step: " + step.name());
                append(runId, "ACTOR_DONE", actor.id() + " -> " + step.name());
            } else {
                append(runId, "LOCAL_STEP", step.name());
            }
            append(runId, "STEP_DONE", step.name());
        }

        append(runId, "DONE", workflowName);
        return new WorkflowRun(runId, workflowName, List.copyOf(journal));
    }

    public List<JournalEntry> journal() {
        return List.copyOf(journal);
    }

    private void append(String runId, String event, String detail) {
        journal.add(new JournalEntry(runId, event, detail, Instant.now()));
    }

    public record WorkflowStep(String name, boolean spawnSubAgent, String agentType, String instruction) {
        public static WorkflowStep local(String name) {
            return new WorkflowStep(name, false, "local", "");
        }

        public static WorkflowStep agent(String name, String agentType, String instruction) {
            return new WorkflowStep(name, true, agentType, instruction);
        }
    }

    public record WorkflowRun(String id, String name, List<JournalEntry> journal) {
    }

    public record JournalEntry(String runId, String event, String detail, Instant createdAt) {
    }
}
