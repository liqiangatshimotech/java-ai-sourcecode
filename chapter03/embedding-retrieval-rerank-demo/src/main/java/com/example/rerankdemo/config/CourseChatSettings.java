package com.example.rerankdemo.config;

import java.util.Map;

/**
 * Chat 模型配置。
 *
 * <p>召回和重排解决“把哪些材料交给模型”的问题；Chat 模型解决“如何组织答案”的问题。
 * 两层配置分开，才能独立优化召回质量和回答质量。
 */
public record CourseChatSettings(
    ChatProvider provider,
    String model,
    String baseUrl,
    String apiKey
) {

    public static CourseChatSettings fromEnvironment() {
        return from(System.getenv());
    }

    static CourseChatSettings from(Map<String, String> env) {
        ChatProvider provider = ChatProvider.from(env.getOrDefault("APP_AI_DEFAULT_PROVIDER", "deepseek"));

        return switch (provider) {
            case DEEPSEEK -> new CourseChatSettings(
                provider,
                env.getOrDefault("DEEPSEEK_MODEL", "deepseek-v4-flash"),
                env.getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
                env.getOrDefault("DEEPSEEK_API_KEY", "")
            );
            case OLLAMA -> new CourseChatSettings(
                provider,
                env.getOrDefault("OLLAMA_CHAT_MODEL", "deepseek-r1:8b"),
                env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"),
                env.getOrDefault("OLLAMA_API_KEY", "")
            );
            case CHATGPT -> new CourseChatSettings(
                provider,
                env.getOrDefault("CHATGPT_MODEL", "gpt-4.1-mini"),
                env.getOrDefault("CHATGPT_BASE_URL", "https://api.openai.com/v1"),
                env.getOrDefault("CHATGPT_API_KEY", "")
            );
        };
    }

    public String safeSummary() {
        return "provider=%s, model=%s, baseUrl=%s".formatted(provider, model, baseUrl);
    }
}
