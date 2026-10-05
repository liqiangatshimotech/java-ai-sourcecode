package com.example.agentscope2.sdk.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenAiSdkSettingsTest {

    @Test
    void defaultsToDeepSeek() {
        OpenAiSdkSettings settings = OpenAiSdkSettings.from(Map.of("DEEPSEEK_API_KEY", "sk-demo"));

        assertEquals(AiProvider.DEEPSEEK, settings.provider());
        assertEquals("deepseek-v4-flash", settings.modelName());
        assertEquals("https://api.deepseek.com/v1", settings.baseUrl());
        assertTrue(settings.hasUsableCredential());
    }

    @Test
    void ollamaDoesNotRequireRealApiKey() {
        OpenAiSdkSettings settings = OpenAiSdkSettings.from(Map.of("AI_PROVIDER", "ollama"));

        assertEquals(AiProvider.OLLAMA, settings.provider());
        assertEquals("qwen3", settings.modelName());
        assertEquals("ollama", settings.apiKey());
        assertTrue(settings.hasUsableCredential());
    }
}
