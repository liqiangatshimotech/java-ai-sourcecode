package com.example.chapter09.mimocode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一个极简任务台账。真实 Coding Agent 会把任务拆给子 Agent，
 * 子 Agent 再把进度写回 progress.md。
 */
public final class TaskBoard {
    private final Map<String, TaskItem> tasks = new LinkedHashMap<>();

    public TaskItem create(String id, String summary) {
        TaskItem task = new TaskItem(id, summary, TaskStatus.OPEN, "");
        tasks.put(id, task);
        return task;
    }

    public void markInProgress(String id) {
        update(id, TaskStatus.IN_PROGRESS, "Task started by a sub-agent.");
    }

    public void markDone(String id, String progress) {
        update(id, TaskStatus.DONE, progress);
    }

    public Map<String, TaskItem> tasks() {
        return Map.copyOf(tasks);
    }

    private void update(String id, TaskStatus status, String progress) {
        TaskItem current = tasks.get(id);
        if (current == null) {
            throw new IllegalArgumentException("Unknown task: " + id);
        }
        tasks.put(id, new TaskItem(id, current.summary(), status, progress));
    }

    public enum TaskStatus {
        OPEN,
        IN_PROGRESS,
        DONE
    }

    public record TaskItem(String id, String summary, TaskStatus status, String progress) {
    }
}
