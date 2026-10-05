package com.example.agentscope2.bailian.config;

import java.util.Locale;

/**
 * 这个 demo 支持的模型供应商。
 *
 * <p>默认 provider 是 DeepSeek，符合课程示例的一贯约定。阿里云百炼通过 `bailian` 或 `dashscope`
 * 显式启用，便于在同一套业务代码里比较不同模型服务的接入差异。
 */
public enum AiProvider {
    DEEPSEEK,
    BAILIAN,
    CHATGPT,
    OLLAMA;

    public static AiProvider from(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "", "deepseek", "deepseek-v4-flash" -> DEEPSEEK;
            case "bailian", "dashscope", "aliyun", "qwen" -> BAILIAN;
            case "chatgpt", "openai", "gpt" -> CHATGPT;
            case "ollama", "local" -> OLLAMA;
            default -> throw new IllegalArgumentException("Unsupported AI_PROVIDER: " + rawValue);
        };
    }
}
