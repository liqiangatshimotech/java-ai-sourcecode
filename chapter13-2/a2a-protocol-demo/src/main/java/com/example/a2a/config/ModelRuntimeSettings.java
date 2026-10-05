package com.example.a2a.config;

import java.util.Map;

/**
 * A2A Agent 背后的模型运行配置。
 *
 * <p>示例项目默认不真实调用模型，但协议服务最终一定会落到某个 Agent Executor。
 * 这里用环境变量把 DeepSeek、Ollama 和 ChatGPT 的配置入口提前留好。
 */
public record ModelRuntimeSettings(
        AiProvider provider,
        String apiKey,
        String modelName,
        String baseUrl) {

    public static ModelRuntimeSettings fromEnvironment() {
        return from(System.getenv());
    }

    public static ModelRuntimeSettings from(Map<String, String> env) {
        AiProvider provider = AiProvider.from(env.getOrDefault("AI_PROVIDER", "deepseek"));
        return switch (provider) {
            case DEEPSEEK -> new ModelRuntimeSettings(
                    provider,
                    env.getOrDefault("DEEPSEEK_API_KEY", ""),
                    env.getOrDefault("DEEPSEEK_MODEL", "deepseek-v4-flash"),
                    env.getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"));
            case OLLAMA -> new ModelRuntimeSettings(
                    provider,
                    "",
                    env.getOrDefault("OLLAMA_MODEL", "qwen3"),
                    env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"));
            case CHATGPT -> new ModelRuntimeSettings(
                    provider,
                    env.getOrDefault("OPENAI_API_KEY", ""),
                    env.getOrDefault("OPENAI_MODEL", "gpt-4o-mini"),
                    env.getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"));
        };
    }

    public boolean hasUsableCredential() {
        return provider == AiProvider.OLLAMA || (apiKey != null && !apiKey.isBlank());
    }

    public String summary() {
        return "provider=%s, model=%s, baseUrl=%s".formatted(provider, modelName, baseUrl);
    }
}
