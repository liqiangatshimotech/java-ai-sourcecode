package com.example.a2a.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelRuntimeSettingsTest {

    @Test
    void defaultsToDeepSeek() {
        ModelRuntimeSettings settings = ModelRuntimeSettings.from(Map.of());

        assertEquals(AiProvider.DEEPSEEK, settings.provider());
        assertEquals("deepseek-v4-flash", settings.modelName());
        assertEquals("https://api.deepseek.com/v1", settings.baseUrl());
        assertFalse(settings.hasUsableCredential());
    }

    @Test
    void supportsOllamaWithoutApiKey() {
        ModelRuntimeSettings settings = ModelRuntimeSettings.from(Map.of(
                "AI_PROVIDER", "ollama",
                "OLLAMA_MODEL", "qwen3"));

        assertEquals(AiProvider.OLLAMA, settings.provider());
        assertEquals("qwen3", settings.modelName());
        assertTrue(settings.hasUsableCredential());
    }

    @Test
    void supportsChatGptConfiguration() {
        ModelRuntimeSettings settings = ModelRuntimeSettings.from(Map.of(
                "AI_PROVIDER", "chatgpt",
                "OPENAI_API_KEY", "sk-test"));

        assertEquals(AiProvider.CHATGPT, settings.provider());
        assertEquals("gpt-4o-mini", settings.modelName());
        assertTrue(settings.hasUsableCredential());
    }
}
