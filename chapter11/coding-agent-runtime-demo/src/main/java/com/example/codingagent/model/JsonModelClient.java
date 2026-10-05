package com.example.codingagent.model;

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
 * 一个严格的 Chat Completions JSON 适配器。
 * 业务层只消费经过校验的对象；网络错误、拒答、截断不能冒充规则模型的成功结果。
 * API Key 仅进入 Authorization 头，不进入请求正文、异常消息或 toString。
 */
public final class JsonModelClient {
    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();

    public JsonModelClient(String baseUrl, String apiKey, String model, boolean local, Duration timeout) {
        String base = baseUrl.replaceAll("/+$", "");
        this.endpoint = URI.create(base.endsWith("/v1") ? base + "/chat/completions" : base + "/v1/chat/completions");
        if (!List.of("http", "https").contains(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.getUserInfo() != null || endpoint.getQuery() != null || endpoint.getFragment() != null) {
            throw new IllegalArgumentException("模型地址必须是无内嵌凭据的 HTTP(S) base URL");
        }
        if (!local && (apiKey == null || apiKey.isBlank())) {
            throw new IllegalStateException("真实模型模式缺少 API Key；配置密钥或显式关闭 RUN_MODEL_CALL");
        }
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model;
        this.timeout = timeout;
    }

    public JsonNode generate(String system, String prompt) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "model", model, "temperature", 0.2, "max_tokens", 4096,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(Map.of("role", "system", "content", system),
                                        Map.of("role", "user", "content", prompt))));
            HttpRequest.Builder request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (!apiKey.isBlank()) request.header("Authorization", "Bearer " + apiKey);
            HttpResponse<byte[]> response = http.send(request.build(), info -> new BoundedBodySubscriber(1_000_000));
            // 不把供应商原始错误体塞进异常：其中可能回显 Prompt 或其他敏感材料。
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException("MODEL_HTTP_" + response.statusCode());
            JsonNode choice = mapper.readTree(response.body()).path("choices").path(0);
            if (!"stop".equals(choice.path("finish_reason").asText()))
                throw new IllegalStateException("MODEL_INCOMPLETE_RESPONSE");
            JsonNode message = choice.path("message");
            if (!message.path("refusal").isMissingNode() && !message.path("refusal").isNull()
                    && !message.path("refusal").asText().isBlank())
                throw new IllegalStateException("MODEL_REFUSAL");
            JsonNode output = mapper.readTree(message.path("content").asText());
            if (output == null || !output.isObject()) throw new IllegalStateException("MODEL_EXPECTED_JSON_OBJECT");
            return output;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("MODEL_INTERRUPTED");
        } catch (java.net.http.HttpTimeoutException ex) {
            throw new IllegalStateException("MODEL_TIMEOUT");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("MODEL_IO_OR_INVALID_JSON");
        }
    }

    /** 类型和长度校验在 Java 中完成，不能只依赖模型宣称自己满足格式。 */
    public static String requiredText(JsonNode object, String field, int limit) {
        JsonNode value = object.path(field);
        if (!value.isTextual() || value.asText().isBlank() || value.asText().length() > limit)
            throw new IllegalStateException("MODEL_INVALID_FIELD_" + field);
        return value.asText();
    }
}
