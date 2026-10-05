package com.example.a2a.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.a2a.client.A2aClient;
import com.example.a2a.protocol.A2aJson;
import com.example.a2a.protocol.AgentCard;
import com.example.a2a.protocol.SendMessageResponse;
import com.example.a2a.protocol.Task;
import com.example.a2a.protocol.TaskState;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class A2aHttpServerTest {

    @Test
    void discoversCardAndCompletesTask() throws Exception {
        try (A2aHttpServer server = new A2aHttpServer("127.0.0.1", 0)) {
            server.start();
            A2aClient client = new A2aClient(server.baseUri(), "test-token");

            AgentCard card = client.discoverAgentCard();
            assertEquals("customer-support-a2a-agent", card.name());
            assertEquals("1.0", card.protocolVersion());
            assertEquals("客服工单分诊", card.skills().getFirst().name());

            SendMessageResponse response = client.sendMessage("请处理工单 T-1001，用户反馈 DeepSeek 网关 504。");
            assertEquals(TaskState.TASK_STATE_COMPLETED, response.task().status().state());

            Task fetched = client.getTask(response.task().id());
            assertEquals(response.task().id(), fetched.id());
            assertTrue(fetched.artifacts().getFirst().firstText().contains("DeepSeek 网关 504"));
        }
    }

    @Test
    void returnsA2aJsonErrorForMissingTask() throws Exception {
        try (A2aHttpServer server = new A2aHttpServer("127.0.0.1", 0)) {
            server.start();
            HttpRequest request = HttpRequest.newBuilder(server.baseUri().resolve("/tasks/not-found"))
                    .header("A2A-Version", "1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            Map<?, ?> body = A2aJson.MAPPER.readValue(response.body(), Map.class);

            assertEquals(404, response.statusCode());
            assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/a2a+json"));
            assertTrue(body.containsKey("error"));
        }
    }
}
