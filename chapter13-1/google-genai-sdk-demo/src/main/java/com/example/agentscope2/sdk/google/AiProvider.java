package com.example.agentscope2.sdk.google;

import java.util.Locale;

/**
 * Google Gen AI SDK 是本篇新增的官方 SDK adapter。
 *
 * <p>课程代码仍然保留 DeepSeek、Ollama 和 ChatGPT 三个常用入口，目的是让业务代码依赖统一接口，而不是直接散落
 * `Client`、`HttpClient`、API Key 这类供应商细节。
 */
public enum AiProvider {
    DEEPSEEK,
    OLLAMA,
    CHATGPT,
    GOOGLE;

    public static AiProvider from(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "", "deepseek", "deepseek-v4-flash" -> DEEPSEEK;
            case "ollama", "local" -> OLLAMA;
            case "chatgpt", "openai", "gpt" -> CHATGPT;
            case "google", "gemini", "genai" -> GOOGLE;
            default -> throw new IllegalArgumentException("Unsupported AI_PROVIDER: " + rawValue);
        };
    }
}
