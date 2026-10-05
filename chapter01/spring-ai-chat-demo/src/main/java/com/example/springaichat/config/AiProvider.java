package com.example.springaichat.config;

import java.util.Locale;

/**
 * Supported model providers for this first Spring AI application.
 */
public enum AiProvider {

    DEEPSEEK,
    OLLAMA,
    OPENAI;

    /**
     * Parse provider names from HTTP requests or configuration.
     * Lowercase values such as "ollama" and "openai" are accepted for user convenience.
     */
    public static AiProvider parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        try {
            return "chatgpt".equalsIgnoreCase(raw.trim()) ? OPENAI : AiProvider.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("provider only supports deepseek, ollama, openai or chatgpt");
        }
    }

    /**
     * Use the configured default provider when the client does not specify one.
     */
    public static AiProvider parseOrDefault(String raw, AiProvider defaultProvider) {
        if (raw == null || raw.isBlank()) {
            return defaultProvider;
        }
        return parse(raw);
    }
}
