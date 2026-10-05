package com.example.langgraph4j.aftersales.config;

import java.util.Locale;

/** 支持的模型供应商。ChatGPT 在配置层对应 OpenAI。 */
public enum AiProvider {
    DEEPSEEK,
    OLLAMA,
    OPENAI;

    public static AiProvider from(String value) {
        if (value == null || value.isBlank()) {
            return DEEPSEEK;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return "CHATGPT".equals(normalized) ? OPENAI : AiProvider.valueOf(normalized);
    }
}
