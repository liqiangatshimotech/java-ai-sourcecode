package com.example.agentscope2.sdk.google;

import java.util.Map;

/**
 * Google Gen AI SDK demo 的统一配置。
 *
 * <p>默认 provider 仍然是 DeepSeek。只有设置 `AI_PROVIDER=google` 时，才会使用 Google 官方 SDK。
 */
public record GoogleGenAiSettings(
        AiProvider provider,
        String apiKey,
        String modelName,
        String baseUrl) {

    public static GoogleGenAiSettings fromEnvironment() {
        return from(System.getenv());
    }

    public static GoogleGenAiSettings from(Map<String, String> env) {
        AiProvider provider = AiProvider.from(env.getOrDefault("AI_PROVIDER", "deepseek"));
        return switch (provider) {
            case DEEPSEEK -> new GoogleGenAiSettings(
                    provider,
                    env.getOrDefault("DEEPSEEK_API_KEY", ""),
                    env.getOrDefault("DEEPSEEK_MODEL", "deepseek-v4-flash"),
                    env.getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"));
            case OLLAMA -> new GoogleGenAiSettings(
                    provider,
                    env.getOrDefault("OLLAMA_API_KEY", "ollama"),
                    env.getOrDefault("OLLAMA_MODEL", "qwen3"),
                    env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434/v1"));
            case CHATGPT -> new GoogleGenAiSettings(
                    provider,
                    env.getOrDefault("OPENAI_API_KEY", ""),
                    env.getOrDefault("OPENAI_MODEL", "gpt-5.5"),
                    env.getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"));
            case GOOGLE -> new GoogleGenAiSettings(
                    provider,
                    firstNonBlank(env.get("GEMINI_API_KEY"), env.get("GOOGLE_API_KEY")),
                    env.getOrDefault("GEMINI_MODEL", "gemini-2.5-flash"),
                    env.getOrDefault("GEMINI_BASE_URL", "https://generativelanguage.googleapis.com"));
        };
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
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
            case GOOGLE -> "GEMINI_API_KEY or GOOGLE_API_KEY";
            case OLLAMA -> "OLLAMA_API_KEY";
        };
    }

    public String summary() {
        return "provider=%s, model=%s, baseUrl=%s".formatted(provider, modelName, baseUrl);
    }
}
