package com.example.embeddingdemo.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseChatSettingsTest {

    @Test
    void defaultsToDeepSeekForCourseChatModel() {
        CourseChatSettings settings = CourseChatSettings.from(Map.of());

        assertEquals(ChatProvider.DEEPSEEK, settings.provider());
        assertEquals("deepseek-v4-flash", settings.model());
    }

    @Test
    void keepsOllamaAndChatgptSwitchesAvailable() {
        CourseChatSettings ollama = CourseChatSettings.from(Map.of("APP_AI_DEFAULT_PROVIDER", "ollama"));
        CourseChatSettings chatgpt = CourseChatSettings.from(Map.of("APP_AI_DEFAULT_PROVIDER", "chatgpt"));

        assertEquals(ChatProvider.OLLAMA, ollama.provider());
        assertEquals(ChatProvider.CHATGPT, chatgpt.provider());
    }
}
