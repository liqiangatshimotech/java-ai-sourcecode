package com.example.a2a.server;

import com.example.a2a.protocol.Artifact;
import com.example.a2a.protocol.Message;
import com.example.a2a.protocol.SendMessageRequest;
import com.example.a2a.protocol.Task;
import com.example.a2a.protocol.TaskState;
import com.example.a2a.protocol.TaskStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 最小 A2A Agent 执行器。
 *
 * <p>这里没有真实请求大模型，而是用确定性规则模拟一个客服 Agent。这样协议层可以离线测试。
 * 后续接 DeepSeek、Ollama、ChatGPT 或 AgentScope Runtime 时，只需要替换这个类。
 */
public class CustomerSupportAgentExecutor {

    private final InMemoryTaskStore taskStore;

    public CustomerSupportAgentExecutor(InMemoryTaskStore taskStore) {
        this.taskStore = taskStore;
    }

    public Task handle(SendMessageRequest request) {
        if (request.message() == null) {
            throw new IllegalArgumentException("message is required");
        }

        String taskId = request.message().taskId() == null
                ? UUID.randomUUID().toString()
                : request.message().taskId();
        String contextId = request.message().contextId() == null
                ? UUID.randomUUID().toString()
                : request.message().contextId();
        String userText = request.message().firstText();

        Task task;
        if (userText.contains("T-1001") || userText.toLowerCase().contains("504")) {
            task = completedTicketTask(taskId, contextId, request.message(), userText);
        } else {
            task = inputRequiredTask(taskId, contextId, request.message());
        }

        taskStore.save(task);
        return task;
    }

    private Task completedTicketTask(String taskId, String contextId, Message userMessage, String userText) {
        String summary = """
                工单 T-1001 建议进入平台稳定性小组。
                原因：用户描述里包含 DeepSeek 网关 504，属于在线客服摘要生成链路的稳定性问题。
                下一步：先确认 504 影响范围，再检查模型网关和重试策略，必要时通知客服侧临时降级。
                """;

        Artifact artifact = Artifact.text("Ticket Triage Result", summary.strip());
        Message statusMessage = Message.agentText(contextId, taskId, "工单分诊已完成。");
        return new Task(
                taskId,
                contextId,
                TaskStatus.of(TaskState.TASK_STATE_COMPLETED, statusMessage),
                List.of(artifact),
                List.of(userMessage, statusMessage),
                Map.of("source", "deterministic-demo", "input", userText));
    }

    private Task inputRequiredTask(String taskId, String contextId, Message userMessage) {
        Message clarification = Message.agentText(
                contextId,
                taskId,
                "需要补充工单号或故障现象，例如 T-1001 或 DeepSeek 网关 504。");
        return new Task(
                taskId,
                contextId,
                TaskStatus.of(TaskState.TASK_STATE_INPUT_REQUIRED, clarification),
                List.of(),
                List.of(userMessage, clarification),
                Map.of("source", "deterministic-demo"));
    }
}
