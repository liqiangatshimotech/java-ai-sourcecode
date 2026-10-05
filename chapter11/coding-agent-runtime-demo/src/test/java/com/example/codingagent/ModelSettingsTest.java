package com.example.codingagent;

import com.example.codingagent.config.AiProvider;
import com.example.codingagent.config.ModelSettings;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelSettingsTest {

    @Test
    void shouldDefaultToDeepSeek() {
        ModelSettings settings = ModelSettings.from(Map.of());

        assertEquals(AiProvider.DEEPSEEK, settings.provider());
        assertEquals("deepseek-v4-flash", settings.model());
        assertFalse(settings.apiKeyConfigured());
    }

    @Test
    void shouldLoadChatGptSettings() {
        ModelSettings settings = ModelSettings.from(Map.of(
                "AI_PROVIDER", "chatgpt",
                "OPENAI_MODEL", "gpt-4o-mini",
                "OPENAI_API_KEY", "sk-test"
        ));

        assertEquals(AiProvider.CHATGPT, settings.provider());
        assertEquals("gpt-4o-mini", settings.model());
        assertTrue(settings.apiKeyConfigured());
    }
}
