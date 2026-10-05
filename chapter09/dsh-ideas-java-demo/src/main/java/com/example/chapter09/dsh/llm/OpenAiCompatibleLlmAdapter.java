package com.example.chapter09.dsh.llm;

import com.example.chapter09.dsh.agent.Message;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAI 兼容适配器：DeepSeek、Ollama、ChatGPT 共用一份实现。
 *
 * 三家的 chat/completions 接口都兼容 OpenAI 协议，所以“支持三家”
 * 在 seam 架构里就是“一个 Provider 类 + 三份配置”。demo 默认不启用它
 * （用 ReplayLlmProvider），这里保留完整可用的调用路径：
 * 设置 DEEPSEEK_API_KEY / OPENAI_API_KEY 后把 provider 换成它即可。
 *
 * 注：解析响应用了容错正则，只取 choices[0].message.content，
 * 生产代码请换成真正的 JSON 库（课程第 2 章讲过结构化输出时的处理）。
 */
public final class OpenAiCompatibleLlmAdapter implements LlmSeam {

    private static final Pattern CONTENT_PATTERN =
            Pattern.compile("\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final AiProviderConfig config;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public OpenAiCompatibleLlmAdapter(AiProviderConfig config) {
        this.config = config;
    }

    @Override
    public LlmReply complete(String systemPrompt, List<Message> messages) {
        StringBuilder body = new StringBuilder();
        body.append("{\"model\":\"").append(config.model()).append("\",\"messages\":[");
        body.append("{\"role\":\"system\",\"content\":").append(json(systemPrompt)).append("}");
        for (Message message : messages) {
            body.append(",{\"role\":\"").append(message.role()).append("\",\"content\":")
                    .append(json(message.content())).append("}");
        }
        body.append("]}");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + System.getenv(
                        "deepseek".equals(config.provider()) ? "DEEPSEEK_API_KEY" : "OPENAI_API_KEY"))
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("LLM HTTP " + response.statusCode()
                        + ": " + response.body());
            }
            return LlmReply.text(extractContent(response.body()));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("LLM request failed", e);
        }
    }

    private static String extractContent(String responseBody) {
        Matcher matcher = CONTENT_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            throw new IllegalStateException("cannot parse model reply: " + responseBody);
        }
        return matcher.group(1)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static String json(String text) {
        return "\"" + text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n") + "\"";
    }

    @Override
    public String providerName() {
        return "llm-" + config.provider();
    }
}
