package com.example.chapter09.signal;

import java.util.Locale;

public record ModelSettings(AiProvider provider, String model, String baseUrl) {

    public static ModelSettings fromEnv() {
        // 课程里的 Java 示例默认使用 DeepSeek。这里虽然不真正请求模型，
        // 但仍保留统一配置，因为真实 Coding Agent 会在同一层组装 system prompt。
        AiProvider provider = AiProvider.parseOrDefault(System.getenv("APP_AI_DEFAULT_PROVIDER"));
        return switch (provider) {
            case DEEPSEEK -> new ModelSettings(
                    provider,
                    env("DEEPSEEK_MODEL", "deepseek-v4-flash"),
                    env("DEEPSEEK_BASE_URL", "https://api.deepseek.com"));
            case OLLAMA -> new ModelSettings(
                    provider,
                    env("OLLAMA_MODEL", "qwen2.5:7b"),
                    env("OLLAMA_BASE_URL", "http://localhost:11434"));
            case CHATGPT -> new ModelSettings(
                    provider,
                    env("CHATGPT_MODEL", "gpt-4.1-mini"),
                    env("CHATGPT_BASE_URL", "https://api.openai.com/v1"));
        };
    }

    public String summary() {
        return provider.id() + "/" + model + " @ " + baseUrl;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    public enum AiProvider {
        DEEPSEEK("deepseek"),
        OLLAMA("ollama"),
        CHATGPT("chatgpt");

        private final String id;

        AiProvider(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static AiProvider parseOrDefault(String raw) {
            if (raw == null || raw.isBlank()) {
                return DEEPSEEK;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            for (AiProvider provider : values()) {
                if (provider.id.equals(normalized)) {
                    return provider;
                }
            }
            throw new IllegalArgumentException("provider only supports deepseek, ollama, chatgpt");
        }
    }
}
