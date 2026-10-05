package com.example.chapter09;

import java.util.Locale;

public record ModelSettings(AiProvider provider, String model, String baseUrl) {

    public static ModelSettings fromEnv() {
        // 课程代码默认走 DeepSeek。需要切换到本地 Ollama 或 ChatGPT 时，只改环境变量，
        // 业务代码仍然依赖同一个 ModelSettings，后续接入真实 ChatClient 时也不需要改调用方。
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
        // 保留 fallback 是为了让 demo 可以开箱运行；生产环境可以在启动阶段强制校验密钥和模型名。
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
            // 未显式指定时固定使用 DeepSeek，保持整套课程示例的默认模型一致。
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
