package com.example.a2a.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.a2a.protocol.Message;
import com.example.a2a.protocol.SendMessageRequest;
import com.example.a2a.protocol.Task;
import com.example.a2a.protocol.TaskState;
import org.junit.jupiter.api.Test;

class CustomerSupportAgentExecutorTest {

    @Test
    void createsCompletedTaskWithArtifactForKnownTicket() {
        InMemoryTaskStore store = new InMemoryTaskStore();
        CustomerSupportAgentExecutor executor = new CustomerSupportAgentExecutor(store);

        Task task = executor.handle(new SendMessageRequest(
                null,
                Message.userText("请处理工单 T-1001，用户反馈 DeepSeek 网关 504。"),
                null,
                null));

        assertEquals(TaskState.TASK_STATE_COMPLETED, task.status().state());
        assertEquals(1, task.artifacts().size());
        assertTrue(task.artifacts().getFirst().firstText().contains("平台稳定性小组"));
        assertTrue(store.findById(task.id()).isPresent());
    }

    @Test
    void asksForMoreInputWhenTicketIsMissing() {
        InMemoryTaskStore store = new InMemoryTaskStore();
        CustomerSupportAgentExecutor executor = new CustomerSupportAgentExecutor(store);

        Task task = executor.handle(new SendMessageRequest(
                null,
                Message.userText("帮我处理一个客服问题。"),
                null,
                null));

        assertEquals(TaskState.TASK_STATE_INPUT_REQUIRED, task.status().state());
        assertTrue(task.status().message().firstText().contains("需要补充工单号"));
    }
}
