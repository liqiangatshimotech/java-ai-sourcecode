package com.example.agentscope2.config;

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
        CourseModelSettings settings = CourseModelSettings.from(Map.of(
                "AI_PROVIDER", "ollama",
                "OLLAMA_MODEL", "qwen3"));

        assertEquals(AiProvider.OLLAMA, settings.provider());
        assertEquals("ollama:qwen3", settings.modelId());
        assertTrue(settings.hasUsableCredential());
    }

    @Test
    void supportsChatGptConfiguration() {
        CourseModelSettings settings = CourseModelSettings.from(Map.of(
                "AI_PROVIDER", "chatgpt",
                "OPENAI_API_KEY", "sk-test",
                "OPENAI_MODEL", "gpt-4o-mini"));

        assertEquals(AiProvider.CHATGPT, settings.provider());
        assertEquals("openai:gpt-4o-mini", settings.modelId());
        assertTrue(settings.hasUsableCredential());
    }
}
