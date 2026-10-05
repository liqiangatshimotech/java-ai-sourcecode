package com.example.springaimcpclient.config;

import java.util.Locale;

public enum AiProvider {

    DEEPSEEK,
    OLLAMA,
    CHATGPT;

    public static AiProvider parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        try {
            return AiProvider.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("provider only supports deepseek, ollama or chatgpt", ex);
        }
    }

    public static AiProvider parseOrDefault(String raw, AiProvider defaultProvider) {
        if (raw == null || raw.isBlank()) {
            return defaultProvider;
        }
        return parse(raw);
    }
}
