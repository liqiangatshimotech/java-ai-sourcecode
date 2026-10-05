package com.example.codingagent.config;

import java.util.Locale;

/**
 * 课程里的所有示例默认用 DeepSeek，同时保留本地 Ollama 和 ChatGPT/OpenAI 的配置入口。
 * 这里不直接创建 HTTP 客户端，只负责把供应商相关的环境变量名称收口到一个地方。
 */
public enum AiProvider {

    DEEPSEEK("deepseek", "DEEPSEEK_MODEL", "deepseek-v4-flash", "DEEPSEEK_BASE_URL",
            "https://api.deepseek.com", "DEEPSEEK_API_KEY"),

    OLLAMA("ollama", "OLLAMA_MODEL", "qwen3", "OLLAMA_BASE_URL",
            "http://localhost:11434/v1", "OLLAMA_API_KEY"),

    CHATGPT("chatgpt", "OPENAI_MODEL", "gpt-4o-mini", "OPENAI_BASE_URL",
            "https://api.openai.com/v1", "OPENAI_API_KEY");

    private final String id;
    private final String modelEnv;
    private final String defaultModel;
    private final String baseUrlEnv;
    private final String defaultBaseUrl;
    private final String apiKeyEnv;

    AiProvider(String id, String modelEnv, String defaultModel, String baseUrlEnv,
               String defaultBaseUrl, String apiKeyEnv) {
        this.id = id;
        this.modelEnv = modelEnv;
        this.defaultModel = defaultModel;
        this.baseUrlEnv = baseUrlEnv;
        this.defaultBaseUrl = defaultBaseUrl;
        this.apiKeyEnv = apiKeyEnv;
    }

    public String id() {
        return id;
    }

    public String modelEnv() {
        return modelEnv;
    }

    public String defaultModel() {
        return defaultModel;
    }

    public String baseUrlEnv() {
        return baseUrlEnv;
    }

    public String defaultBaseUrl() {
        return defaultBaseUrl;
    }

    public String apiKeyEnv() {
        return apiKeyEnv;
    }

    public static AiProvider from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return DEEPSEEK;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("openai")) return CHATGPT;
        for (AiProvider provider : values()) {
            if (provider.id.equals(normalized)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unsupported AI_PROVIDER: " + rawValue);
    }
}
