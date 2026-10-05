package com.example.agentscope2.config;

import java.util.Locale;

/**
 * 课程示例支持的模型供应商。
 *
 * <p>这里没有把 DeepSeek 当成独立 SDK 处理，因为 DeepSeek 提供 OpenAI 兼容接口，在 AgentScope Java 2.0
 * 里可以通过 OpenAI 扩展模型加 DeepSeek formatter 接入。
 */
public enum AiProvider {
    DEEPSEEK,
    CHATGPT,
    OLLAMA;

    public static AiProvider from(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "", "deepseek", "deepseek-v4-flash" -> DEEPSEEK;
            case "chatgpt", "openai", "gpt" -> CHATGPT;
            case "ollama", "local" -> OLLAMA;
            default -> throw new IllegalArgumentException("Unsupported AI_PROVIDER: " + rawValue);
        };
    }
}
