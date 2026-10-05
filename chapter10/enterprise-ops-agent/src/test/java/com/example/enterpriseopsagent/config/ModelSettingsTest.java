package com.example.enterpriseopsagent.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelSettingsTest {

    @Test
    void shouldUseDeepSeekAsDefaultModel() {
        ModelSettings settings = ModelSettings.deepSeekDefault();

        assertEquals(ModelProvider.DEEPSEEK, settings.provider());
        assertEquals("https://api.deepseek.com", settings.baseUrl());
        assertEquals("deepseek-v4-flash", settings.modelName());
        assertTrue(settings.safeSummary().contains("DEEPSEEK"));
    }

    @Test
    void shouldKeepOllamaAndOpenAiConfigurationAvailable() {
        assertEquals(ModelProvider.OLLAMA, ModelSettings.ollamaLocal().provider());
        assertEquals(ModelProvider.OPENAI, ModelSettings.openAiDefault().provider());
    }

    @Test
    void shouldResolveModelProviderFromRuntimeName() {
        assertEquals(ModelProvider.DEEPSEEK, ModelSettings.fromProviderName("").provider());
        assertEquals(ModelProvider.OLLAMA, ModelSettings.fromProviderName("ollama").provider());
        assertEquals(ModelProvider.OPENAI, ModelSettings.fromProviderName("chatgpt").provider());
    }
}
