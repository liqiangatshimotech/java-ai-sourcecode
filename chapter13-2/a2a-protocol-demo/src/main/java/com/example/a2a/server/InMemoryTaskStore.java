package com.example.a2a.server;

import com.example.a2a.protocol.Task;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单内存任务仓库。
 *
 * <p>真实 A2A 服务需要把 Task 存到数据库或状态存储里，才能支持多实例部署、重启恢复和审计。
 */
public class InMemoryTaskStore {

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    public void save(Task task) {
        tasks.put(task.id(), task);
    }

    public Optional<Task> findById(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public Collection<Task> list() {
        return tasks.values();
    }
}
