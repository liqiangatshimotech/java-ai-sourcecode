package com.example.a2a.client;

import com.example.a2a.protocol.A2aJson;
import com.example.a2a.protocol.AgentCard;
import com.example.a2a.protocol.Message;
import com.example.a2a.protocol.SendMessageConfiguration;
import com.example.a2a.protocol.SendMessageRequest;
import com.example.a2a.protocol.SendMessageResponse;
import com.example.a2a.protocol.Task;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * 最小 A2A 客户端。
 */
public class A2aClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final URI baseUri;
    private final String bearerToken;

    public A2aClient(URI baseUri, String bearerToken) {
        this.baseUri = baseUri;
        this.bearerToken = bearerToken;
    }

    public AgentCard discoverAgentCard() throws IOException, InterruptedException {
        HttpRequest request = baseRequest(baseUri.resolve("/.well-known/agent-card.json"))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response);
        return A2aJson.MAPPER.readValue(response.body(), AgentCard.class);
    }

    public SendMessageResponse sendMessage(String text) throws IOException, InterruptedException {
        SendMessageRequest body = new SendMessageRequest(
                null,
                Message.userText(text),
                new SendMessageConfiguration(List.of("application/json", "text/plain"), 3, false),
                null);

        HttpRequest request = baseRequest(baseUri.resolve("/message:send"))
                .header("Content-Type", "application/a2a+json")
                .header("A2A-Version", "1.0")
                .POST(HttpRequest.BodyPublishers.ofString(A2aJson.MAPPER.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response);
        return A2aJson.MAPPER.readValue(response.body(), SendMessageResponse.class);
    }

    public Task getTask(String taskId) throws IOException, InterruptedException {
        HttpRequest request = baseRequest(baseUri.resolve("/tasks/" + taskId))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response);
        return A2aJson.MAPPER.readValue(response.body(), Task.class);
    }

    private HttpRequest.Builder baseRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + bearerToken);
    }

    private static void ensureSuccess(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("A2A request failed: status=%s body=%s"
                    .formatted(response.statusCode(), response.body()));
        }
    }
}
