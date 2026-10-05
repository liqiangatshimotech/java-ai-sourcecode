package com.example.chapter09.mimocode.context;

import java.util.Locale;
import java.util.Map;

/**
 * 课程里的所有 AI 示例都默认以 DeepSeek 为主，同时保留 Ollama 和 ChatGPT
 * 的配置入口。这个 demo 不会真正请求模型，但运行时仍然打印当前 provider，
 * 方便把工程配置和文档里的说明对齐。
 */
public record AiProviderConfig(
        String provider,
        String model,
        String baseUrl,
        String maskedApiKey
) {
    public static AiProviderConfig fromEnvironment(Map<String, String> env) {
        String provider = env.getOrDefault("APP_AI_DEFAULT_PROVIDER", "deepseek")
                .trim()
                .toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "ollama" -> new AiProviderConfig(
                    "ollama",
                    env.getOrDefault("OLLAMA_MODEL", "qwen2.5-coder:7b"),
                    env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"),
                    "(local)");
            case "chatgpt", "openai" -> new AiProviderConfig(
                    "chatgpt",
                    env.getOrDefault("CHATGPT_MODEL", "gpt-4.1-mini"),
                    env.getOrDefault("CHATGPT_BASE_URL", "https://api.openai.com/v1"),
                    mask(env.getOrDefault("CHATGPT_API_KEY", env.get("OPENAI_API_KEY"))));
            default -> new AiProviderConfig(
                    "deepseek",
                    env.getOrDefault("DEEPSEEK_MODEL", "deepseek-v4-flash"),
                    env.getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
                    mask(env.get("DEEPSEEK_API_KEY")));
        };
    }

    /** 只保存配置状态，既不保留原始密钥，也不暴露前后缀。 */
    private static String mask(String apiKey) {
        return apiKey == null || apiKey.isBlank() ? "(not configured)" : "(configured)";
    }
}
