package com.example.agentscope2.sdk.anthropic;

import java.util.Locale;

/**
 * Anthropic Java SDK 是本篇新增的官方 SDK adapter。
 *
 * <p>DeepSeek、Ollama 和 ChatGPT 仍然走课程统一的 OpenAI-compatible 路径，避免业务代码直接绑定某个供应商。
 */
public enum AiProvider {
    DEEPSEEK,
    OLLAMA,
    CHATGPT,
    ANTHROPIC;

    public static AiProvider from(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "", "deepseek", "deepseek-v4-flash" -> DEEPSEEK;
            case "ollama", "local" -> OLLAMA;
            case "chatgpt", "openai", "gpt" -> CHATGPT;
            case "anthropic", "claude" -> ANTHROPIC;
            default -> throw new IllegalArgumentException("Unsupported AI_PROVIDER: " + rawValue);
        };
    }
}
