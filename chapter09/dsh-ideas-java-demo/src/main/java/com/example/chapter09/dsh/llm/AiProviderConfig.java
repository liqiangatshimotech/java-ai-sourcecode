package com.example.chapter09.dsh.llm;

import java.util.Locale;
import java.util.Map;

/**
 * 课程统一的模型配置：默认 DeepSeek，保留 Ollama 和 ChatGPT 入口。
 *
 * demo 默认用 ReplayLlmProvider 离线回放，不真正请求模型；
 * 但配置对象仍然打印当前 provider，保证工程配置和文档口径一致。
 * 想让 OpenAiCompatibleLlmAdapter 真正发请求时，设置对应环境变量即可。
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
                    env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434/v1"),
                    "(local)");
            case "chatgpt", "openai" -> new AiProviderConfig(
                    "chatgpt",
                    env.getOrDefault("CHATGPT_MODEL", "gpt-4.1-mini"),
                    env.getOrDefault("CHATGPT_BASE_URL", "https://api.openai.com/v1"),
                    mask(env.get("OPENAI_API_KEY")));
            default -> new AiProviderConfig(
                    "deepseek",
                    env.getOrDefault("DEEPSEEK_MODEL", "deepseek-v4-flash"),
                    env.getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"),
                    mask(env.get("DEEPSEEK_API_KEY")));
        };
    }

    private static String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "(not configured)";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
