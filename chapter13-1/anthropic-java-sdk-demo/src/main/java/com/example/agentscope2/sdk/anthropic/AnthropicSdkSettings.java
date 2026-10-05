package com.example.agentscope2.sdk.anthropic;

import java.util.Map;

/**
 * Anthropic Java SDK demo 的统一配置。
 *
 * <p>默认 provider 仍然是 DeepSeek。只有设置 `AI_PROVIDER=anthropic` 时，才会使用 Anthropic 官方 SDK。
 */
public record AnthropicSdkSettings(
        AiProvider provider,
        String apiKey,
        String modelName,
        String baseUrl) {

    public static AnthropicSdkSettings fromEnvironment() {
        return from(System.getenv());
    }

    public static AnthropicSdkSettings from(Map<String, String> env) {
        AiProvider provider = AiProvider.from(env.getOrDefault("AI_PROVIDER", "deepseek"));
        return switch (provider) {
            case DEEPSEEK -> new AnthropicSdkSettings(
                    provider,
                    env.getOrDefault("DEEPSEEK_API_KEY", ""),
                    env.getOrDefault("DEEPSEEK_MODEL", "deepseek-v4-flash"),
                    env.getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"));
            case OLLAMA -> new AnthropicSdkSettings(
                    provider,
                    env.getOrDefault("OLLAMA_API_KEY", "ollama"),
                    env.getOrDefault("OLLAMA_MODEL", "qwen3"),
                    env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434/v1"));
            case CHATGPT -> new AnthropicSdkSettings(
                    provider,
                    env.getOrDefault("OPENAI_API_KEY", ""),
                    env.getOrDefault("OPENAI_MODEL", "gpt-5.5"),
                    env.getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"));
            case ANTHROPIC -> new AnthropicSdkSettings(
                    provider,
                    env.getOrDefault("ANTHROPIC_API_KEY", ""),
                    env.getOrDefault("ANTHROPIC_MODEL", "claude-opus-4-8"),
                    env.getOrDefault("ANTHROPIC_BASE_URL", "https://api.anthropic.com"));
        };
    }

    public boolean requiresApiKey() {
        return provider != AiProvider.OLLAMA;
    }

    public boolean hasUsableCredential() {
        return !requiresApiKey() || (apiKey != null && !apiKey.isBlank());
    }

    public String requiredCredentialName() {
        return switch (provider) {
            case DEEPSEEK -> "DEEPSEEK_API_KEY";
            case CHATGPT -> "OPENAI_API_KEY";
            case ANTHROPIC -> "ANTHROPIC_API_KEY";
            case OLLAMA -> "OLLAMA_API_KEY";
        };
    }

    public String summary() {
        return "provider=%s, model=%s, baseUrl=%s".formatted(provider, modelName, baseUrl);
    }
}
