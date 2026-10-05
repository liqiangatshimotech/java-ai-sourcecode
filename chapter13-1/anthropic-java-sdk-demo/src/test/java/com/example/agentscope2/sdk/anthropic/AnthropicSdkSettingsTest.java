package com.example.agentscope2.sdk.anthropic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AnthropicSdkSettingsTest {

    @Test
    void defaultsToDeepSeek() {
        AnthropicSdkSettings settings = AnthropicSdkSettings.from(Map.of("DEEPSEEK_API_KEY", "sk-demo"));

        assertEquals(AiProvider.DEEPSEEK, settings.provider());
        assertEquals("deepseek-v4-flash", settings.modelName());
        assertEquals("https://api.deepseek.com/v1", settings.baseUrl());
        assertTrue(settings.hasUsableCredential());
    }

    @Test
    void supportsAnthropicProvider() {
        AnthropicSdkSettings settings = AnthropicSdkSettings.from(Map.of(
                "AI_PROVIDER", "anthropic",
                "ANTHROPIC_API_KEY", "sk-ant-demo"));

        assertEquals(AiProvider.ANTHROPIC, settings.provider());
        assertEquals("claude-opus-4-8", settings.modelName());
        assertEquals("https://api.anthropic.com", settings.baseUrl());
    }
}
