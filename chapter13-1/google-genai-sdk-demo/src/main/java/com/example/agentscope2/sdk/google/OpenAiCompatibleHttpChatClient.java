package com.example.agentscope2.sdk.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek、Ollama、ChatGPT 的 OpenAI-compatible 兜底 adapter。
 *
 * <p>Google SDK 本身不负责调用 DeepSeek 或 Ollama。为了遵守课程示例的默认模型约定，这里用 JDK 自带
 * `HttpClient` 实现一个极小的 `/chat/completions` 调用，业务层仍然只面对 `ChatClient`。
 */
public final class OpenAiCompatibleHttpChatClient implements ChatClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GoogleGenAiSettings settings;
    private final HttpClient httpClient;

    public OpenAiCompatibleHttpChatClient(GoogleGenAiSettings settings) {
        if (!settings.hasUsableCredential()) {
            throw new IllegalStateException("Missing " + settings.requiredCredentialName());
        }
        this.settings = settings;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String complete(String userText) throws Exception {
        String requestJson = buildRequestJson(settings.modelName(), userText);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(chatCompletionsUrl(settings.baseUrl())))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson));

        if (settings.apiKey() != null && !settings.apiKey().isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + settings.apiKey());
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Model request failed: HTTP " + response.statusCode() + " " + response.body());
        }
        return extractAssistantText(response.body());
    }

    static String buildRequestJson(String modelName, String userText) throws Exception {
        Map<String, Object> body = Map.of(
                "model", modelName,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", "你是一个严谨的 Java AI 应用工程助手，回答要短，先给结论。"),
                        Map.of("role", "user", "content", userText)));
        return MAPPER.writeValueAsString(body);
    }

    static String extractAssistantText(String responseJson) throws Exception {
        JsonNode root = MAPPER.readTree(responseJson);
        return root.path("choices").path(0).path("message").path("content").asText("");
    }

    static String chatCompletionsUrl(String baseUrl) {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalized + "/chat/completions";
    }
}
