package com.example.javamcpreporadar.config;

import java.util.Locale;

public enum AiProvider {
    DEEPSEEK,
    OLLAMA,
    CHATGPT;

    public static AiProvider parseOrDefault(String value, AiProvider defaultProvider) {
        if (value == null || value.isBlank()) {
            return defaultProvider;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "deepseek" -> DEEPSEEK;
            case "ollama" -> OLLAMA;
            case "chatgpt", "openai" -> CHATGPT;
            default -> defaultProvider;
        };
    }
}
