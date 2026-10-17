package com.example.structuredoutput.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** 可选的真实模型适配器：DeepSeek/ChatGPT 用 OpenAI 兼容接口，Ollama 用原生接口。 */
public final class HttpModelClient implements ModelClient {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();
    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final boolean ollama;

    public static HttpModelClient fromEnvironment() {
        return new HttpModelClient(System::getenv);
    }

    /** 配置函数便于测试替换成本机 HTTP 服务，不需要真实密钥或云端额度。 */
    public HttpModelClient(Function<String, String> config) {
        String provider = value(config, "APP_AI_DEFAULT_PROVIDER", "deepseek").toLowerCase(Locale.ROOT);
        if (!List.of("deepseek", "ollama", "chatgpt").contains(provider)) {
            throw new IllegalArgumentException("APP_AI_DEFAULT_PROVIDER 仅支持 deepseek、ollama、chatgpt");
        }
        ollama = provider.equals("ollama");
        String prefix = provider.toUpperCase(Locale.ROOT);
        apiKey = value(config, prefix + "_API_KEY", "");
        if (!ollama && apiKey.isBlank()) throw new IllegalArgumentException("请设置 " + prefix + "_API_KEY");
        String base = value(config, prefix + "_BASE_URL", switch (provider) {
            case "ollama" -> "http://localhost:11434";
            case "chatgpt" -> "https://api.openai.com";
            default -> "https://api.deepseek.com";
        }).replaceAll("/+$", "");
        // 避免 base URL 已有 /v1 时重复拼接。
        if (!ollama) base = base.replaceAll("/v1$", "");
        endpoint = URI.create(base + (ollama ? "/api/chat" : "/v1/chat/completions"));
        model = value(config, prefix + "_MODEL", switch (provider) {
            case "ollama" -> "qwen2.5:7b";
            case "chatgpt" -> "gpt-4.1-mini";
            default -> "deepseek-v4-flash";
        });
    }

    @Override
    public String generate(String prompt) {
        // 只要求模型返回文本，由业务层解析 JSON；这里不依赖供应商专有 JSON Schema 功能。
        Map<String, Object> body = Map.of("model", model, "stream", false,
                "messages", List.of(Map.of("role", "user", "content", prompt)));
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            if (!ollama) request.header("Authorization", "Bearer " + apiKey);
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                // 不打印供应商响应体，以免把输入、密钥或个人信息带进日志。
                throw new IllegalStateException("模型 HTTP 状态：" + response.statusCode());
            }
            JsonNode root = json.readTree(response.body());
            JsonNode content = ollama ? root.path("message").path("content")
                    : root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                throw new IllegalStateException("模型未返回有效文本");
            }
            return content.asText();
        } catch (InterruptedException ex) {
            // 恢复中断标记，重试器看到它会立即停止，不再调用模型。
            Thread.currentThread().interrupt();
            throw new IllegalStateException("模型请求被取消");
        } catch (IOException ex) {
            throw new IllegalStateException("模型网络请求或响应解析失败");
        }
    }

    private static String value(Function<String, String> config, String name, String fallback) {
        String raw = config.apply(name);
        return raw == null || raw.isBlank() ? fallback : raw.trim();
    }
}
