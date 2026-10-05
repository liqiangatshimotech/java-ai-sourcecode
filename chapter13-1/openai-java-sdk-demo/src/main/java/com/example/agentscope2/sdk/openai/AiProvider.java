package com.example.agentscope2.sdk.openai;

import java.util.Locale;

/**
 * OpenAI Java SDK 可以连接 OpenAI 官方接口，也可以连接兼容 OpenAI Chat Completions 协议的服务。
 *
 * <p>课程示例默认选择 DeepSeek，是为了让后续 AgentScope 2.0 示例在同一套环境变量下运行。Ollama 通过
 * `http://localhost:11434/v1` 暴露 OpenAI-compatible 接口，ChatGPT 则使用 OpenAI 官方地址。
 */
public enum AiProvider {
    DEEPSEEK,
    OLLAMA,
    CHATGPT;

    public static AiProvider from(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "", "deepseek", "deepseek-v4-flash" -> DEEPSEEK;
            case "ollama", "local" -> OLLAMA;
            case "chatgpt", "openai", "gpt" -> CHATGPT;
            default -> throw new IllegalArgumentException("Unsupported AI_PROVIDER: " + rawValue);
        };
    }
}
