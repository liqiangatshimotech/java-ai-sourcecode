package com.example.aiproduction.config;

import java.util.Map;

/**
 * 生产化示例使用的模型配置。
 *
 * <p>这里不保存 API Key，是因为第 12 章关注治理层：预算、日志、评测、安全。真实项目里可以把 apiKey 字段加回来，
 * 但依然应该从环境变量或密钥系统读取，不要写死在代码里。
 */
public record ModelSettings(AiProvider provider, String modelName, String baseUrl, boolean stream) {

    public static ModelSettings fromEnvironment() {
        return from(System.getenv());
    }

    static ModelSettings from(Map<String, String> env) {
        AiProvider provider = AiProvider.from(env.getOrDefault("AI_PROVIDER", "deepseek"));
        boolean stream = Boolean.parseBoolean(env.getOrDefault("AI_STREAM", "true"));

        return switch (provider) {
            case DEEPSEEK ->
                    new ModelSettings(
                            provider,
                            env.getOrDefault("DEEPSEEK_MODEL", "deepseek-v4-flash"),
                            env.getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"),
                            stream);
            case OLLAMA ->
                    new ModelSettings(
                            provider,
                            env.getOrDefault("OLLAMA_MODEL", "qwen3"),
                            env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"),
                            stream);
            case CHATGPT ->
                    new ModelSettings(
                            provider,
                            env.getOrDefault("OPENAI_MODEL", "gpt-4o-mini"),
                            env.getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"),
                            stream);
        };
    }

    public String summary() {
        return "provider=%s, model=%s, baseUrl=%s, stream=%s"
                .formatted(provider, modelName, baseUrl, stream);
    }
}
