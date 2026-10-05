package com.example.agentscope2.sdk.google;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class GoogleGenAiSettingsTest {

    @Test
    void defaultsToDeepSeek() {
        GoogleGenAiSettings settings = GoogleGenAiSettings.from(Map.of("DEEPSEEK_API_KEY", "sk-demo"));

        assertEquals(AiProvider.DEEPSEEK, settings.provider());
        assertEquals("deepseek-v4-flash", settings.modelName());
        assertEquals("https://api.deepseek.com/v1", settings.baseUrl());
        assertTrue(settings.hasUsableCredential());
    }

    @Test
    void supportsGeminiApiKeyAlias() {
        GoogleGenAiSettings settings = GoogleGenAiSettings.from(Map.of(
                "AI_PROVIDER", "google",
                "GOOGLE_API_KEY", "google-demo"));

        assertEquals(AiProvider.GOOGLE, settings.provider());
        assertEquals("gemini-2.5-flash", settings.modelName());
        assertEquals("google-demo", settings.apiKey());
    }
}
