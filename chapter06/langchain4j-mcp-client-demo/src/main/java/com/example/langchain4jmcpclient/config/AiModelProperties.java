package com.example.langchain4jmcpclient.config;

import java.util.Map;

public record AiModelProperties(
    AiProvider defaultProvider,
    OpenAiCompatible deepseek,
    Ollama ollama,
    OpenAiCompatible chatgpt
) {

    public static AiModelProperties fromEnv() {
        return fromEnv(System.getenv());
    }

    public static AiModelProperties fromEnv(Map<String, String> env) {
        return new AiModelProperties(
            AiProvider.parseOrDefault(read(env, "APP_AI_DEFAULT_PROVIDER", "deepseek"), AiProvider.DEEPSEEK),
            new OpenAiCompatible(
                read(env, "DEEPSEEK_API_KEY", ""),
                read(env, "DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"),
                read(env, "DEEPSEEK_MODEL", "deepseek-v4-flash"),
                readDouble(env, "DEEPSEEK_TEMPERATURE", 0.2),
                readInt(env, "DEEPSEEK_MAX_TOKENS", 1024)
            ),
            new Ollama(
                read(env, "OLLAMA_BASE_URL", "http://localhost:11434"),
                read(env, "OLLAMA_MODEL", "qwen3.5:9b"),
                readDouble(env, "OLLAMA_TEMPERATURE", 0.2)
            ),
            new OpenAiCompatible(
                read(env, "CHATGPT_API_KEY", read(env, "OPENAI_API_KEY", "")),
                read(env, "CHATGPT_BASE_URL", "https://api.openai.com/v1"),
                read(env, "CHATGPT_MODEL", "gpt-4o-mini"),
                readDouble(env, "CHATGPT_TEMPERATURE", 0.2),
                readInt(env, "CHATGPT_MAX_TOKENS", 1024)
            )
        );
    }

    private static String read(Map<String, String> env, String key, String defaultValue) {
        String value = env.get(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static double readDouble(Map<String, String> env, String key, double defaultValue) {
        String value = env.get(key);
        return value == null || value.isBlank() ? defaultValue : Double.parseDouble(value.trim());
    }

    private static int readInt(Map<String, String> env, String key, int defaultValue) {
        String value = env.get(key);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value.trim());
    }

    public record OpenAiCompatible(
        String apiKey,
        String baseUrl,
        String model,
        double temperature,
        int maxTokens
    ) {

        public boolean configured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Ollama(String baseUrl, String model, double temperature) {
    }
}
