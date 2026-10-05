package com.example.agentscopeframework.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CourseModelSettingsTest {

    @Test
    void defaultsToDeepSeek() {
        CourseModelSettings settings = CourseModelSettings.from(Map.of());

        assertEquals(AiProvider.DEEPSEEK, settings.provider());
        assertEquals("deepseek-v4-flash", settings.modelName());
        assertEquals("https://api.deepseek.com/v1", settings.baseUrl());
        assertFalse(settings.hasUsableCredential());
    }

    @Test
    void supportsOllamaWithoutApiKey() {
        CourseModelSettings settings =
                CourseModelSettings.from(
                        Map.of(
                                "AI_PROVIDER", "ollama",
                                "OLLAMA_MODEL", "qwen3",
                                "OLLAMA_BASE_URL", "http://localhost:11434"));

        assertEquals(AiProvider.OLLAMA, settings.provider());
        assertEquals("qwen3", settings.modelName());
        assertTrue(settings.hasUsableCredential());
    }

    @Test
    void supportsChatGptAlias() {
        CourseModelSettings settings =
                CourseModelSettings.from(
                        Map.of(
                                "AI_PROVIDER", "openai",
                                "OPENAI_API_KEY", "test-key",
                                "OPENAI_MODEL", "gpt-4o-mini"));

        assertEquals(AiProvider.CHATGPT, settings.provider());
        assertEquals("gpt-4o-mini", settings.modelName());
        assertTrue(settings.hasUsableCredential());
    }
}
