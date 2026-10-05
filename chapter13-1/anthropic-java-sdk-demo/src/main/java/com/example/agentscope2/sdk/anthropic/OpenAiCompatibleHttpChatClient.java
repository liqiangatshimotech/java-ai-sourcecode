package com.example.agentscope2.sdk.anthropic;

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
 * DeepSeek、Ollama、ChatGPT 的 OpenAI-compatible adapter。
 *
 * <p>Anthropic SDK 不负责调用这些服务，所以这里把兼容协议调用隔离出来。业务代码仍然使用 `ChatClient`，
 * 后续切到 Anthropic、DeepSeek 或本地模型时不需要改调用处。
 */
public final class OpenAiCompatibleHttpChatClient implements ChatClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AnthropicSdkSettings settings;
    private final HttpClient httpClient;

    public OpenAiCompatibleHttpChatClient(AnthropicSdkSettings settings) {
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
