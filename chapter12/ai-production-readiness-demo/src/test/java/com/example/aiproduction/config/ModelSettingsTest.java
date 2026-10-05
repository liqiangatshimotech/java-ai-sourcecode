package com.example.aiproduction.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelSettingsTest {

    @Test
    void defaultsToDeepSeek() {
        ModelSettings settings = ModelSettings.from(Map.of());

        assertEquals(AiProvider.DEEPSEEK, settings.provider());
        assertEquals("deepseek-v4-flash", settings.modelName());
    }

    @Test
    void supportsOllama() {
        ModelSettings settings = ModelSettings.from(Map.of("AI_PROVIDER", "ollama"));

        assertEquals(AiProvider.OLLAMA, settings.provider());
        assertEquals("qwen3", settings.modelName());
    }
}
