package com.example.chapter09.mimocode;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 子 Agent 注册表。MiMo-Code 里 actor registry 会记录谁被谁拉起、
 * 绑定了哪个 task、是否后台执行。这里保留最关键的字段，方便观察
 * "主 Agent 拆任务 -> 子 Agent 执行 -> 进度回写" 这条链路。
 */
public final class ActorRegistry {
    private final Map<String, ActorRecord> actors = new LinkedHashMap<>();
    private int sequence = 1;

    public ActorRecord spawnSubAgent(String parentSessionId, String taskId, String agentType, String instruction) {
        String id = "A-" + sequence++;
        ActorRecord actor = new ActorRecord(
                id,
                parentSessionId,
                taskId,
                agentType,
                instruction,
                ActorStatus.RUNNING,
                "",
                Instant.now());
        actors.put(id, actor);
        return actor;
    }

    public void markDone(String actorId, String progress) {
        ActorRecord current = actors.get(actorId);
        if (current == null) {
            throw new IllegalArgumentException("Unknown actor: " + actorId);
        }
        actors.put(actorId, new ActorRecord(
                current.id(),
                current.parentSessionId(),
                current.taskId(),
                current.agentType(),
                current.instruction(),
                ActorStatus.DONE,
                progress,
                current.createdAt()));
    }

    public Map<String, ActorRecord> actors() {
        return Map.copyOf(actors);
    }

    public enum ActorStatus {
        RUNNING,
        DONE
    }

    public record ActorRecord(
            String id,
            String parentSessionId,
            String taskId,
            String agentType,
            String instruction,
            ActorStatus status,
            String progress,
            Instant createdAt) {
    }
}
