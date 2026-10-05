package com.example.agentscope2.config;

import java.util.Map;

/**
 * 统一收拢模型配置。
 *
 * <p>示例代码只从环境变量读取 API Key 和 Base URL。这样文档、仓库和 OSS 包里都不会出现真实密钥。
 */
public record CourseModelSettings(
        AiProvider provider,
        String apiKey,
        String modelName,
        String baseUrl,
        boolean stream) {

    public static CourseModelSettings fromEnvironment() {
        return from(System.getenv());
    }

    public static CourseModelSettings from(Map<String, String> env) {
        AiProvider provider = AiProvider.from(env.getOrDefault("AI_PROVIDER", "deepseek"));
        boolean stream = Boolean.parseBoolean(env.getOrDefault("AI_STREAM", "true"));

        return switch (provider) {
            case DEEPSEEK -> new CourseModelSettings(
                    provider,
                    env.getOrDefault("DEEPSEEK_API_KEY", ""),
                    env.getOrDefault("DEEPSEEK_MODEL", "deepseek-v4-flash"),
                    env.getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"),
                    stream);
            case CHATGPT -> new CourseModelSettings(
                    provider,
                    env.getOrDefault("OPENAI_API_KEY", ""),
                    env.getOrDefault("OPENAI_MODEL", "gpt-4o-mini"),
                    env.getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"),
                    stream);
            case OLLAMA -> new CourseModelSettings(
                    provider,
                    "",
                    env.getOrDefault("OLLAMA_MODEL", "qwen3"),
                    env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"),
                    stream);
        };
    }

    public boolean requiresApiKey() {
        return provider != AiProvider.OLLAMA;
    }

    public boolean hasUsableCredential() {
        return !requiresApiKey() || (apiKey != null && !apiKey.isBlank());
    }

    public String modelId() {
        return switch (provider) {
            case DEEPSEEK -> "openai:" + modelName;
            case CHATGPT -> "openai:" + modelName;
            case OLLAMA -> "ollama:" + modelName;
        };
    }

    public String summary() {
        return "provider=%s, model=%s, baseUrl=%s, stream=%s"
                .formatted(provider, modelName, baseUrl, stream);
    }
}
