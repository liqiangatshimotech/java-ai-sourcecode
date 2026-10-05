package com.example.agentscopeframework.config;

import java.util.Map;

/**
 * 统一的模型配置对象。
 *
 * <p>课程示例不直接在代码里写死 API Key，所有敏感信息都从环境变量读取。这样既方便本地切换模型，也避免把密钥提交到代码仓库。
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

    static CourseModelSettings from(Map<String, String> env) {
        AiProvider provider = AiProvider.from(env.getOrDefault("AI_PROVIDER", "deepseek"));
        boolean stream = Boolean.parseBoolean(env.getOrDefault("AI_STREAM", "true"));

        return switch (provider) {
            case DEEPSEEK ->
                    new CourseModelSettings(
                            provider,
                            env.getOrDefault("DEEPSEEK_API_KEY", ""),
                            env.getOrDefault("DEEPSEEK_MODEL", "deepseek-v4-flash"),
                            env.getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"),
                            stream);
            case CHATGPT ->
                    new CourseModelSettings(
                            provider,
                            env.getOrDefault("OPENAI_API_KEY", ""),
                            env.getOrDefault("OPENAI_MODEL", "gpt-4o-mini"),
                            env.getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"),
                            stream);
            case OLLAMA ->
                    new CourseModelSettings(
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

    public String summary() {
        return "provider=%s, model=%s, baseUrl=%s, stream=%s"
                .formatted(provider, modelName, baseUrl, stream);
    }
}
