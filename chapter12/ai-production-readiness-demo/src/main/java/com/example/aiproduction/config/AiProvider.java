package com.example.aiproduction.config;

import java.util.Locale;

/**
 * AI 模型供应商枚举。
 *
 * <p>第 12 章不真实请求模型，但生产化治理代码仍然要知道当前请求准备走哪个供应商。这里默认 DeepSeek，同时保留 Ollama
 * 和 ChatGPT，方便后续把治理层接到真实模型客户端。
 */
public enum AiProvider {
    DEEPSEEK,
    OLLAMA,
    CHATGPT;

    public static AiProvider from(String rawValue) {
        String value = rawValue == null ? "deepseek" : rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "ollama" -> OLLAMA;
            case "chatgpt", "openai" -> CHATGPT;
            default -> DEEPSEEK;
        };
    }
}
