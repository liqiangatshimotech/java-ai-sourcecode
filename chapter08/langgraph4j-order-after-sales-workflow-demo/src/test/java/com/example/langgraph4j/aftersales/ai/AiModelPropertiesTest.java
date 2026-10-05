package com.example.langgraph4j.aftersales.ai;

import com.example.langgraph4j.aftersales.config.AiModelProperties;
import com.example.langgraph4j.aftersales.config.AiProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiModelPropertiesTest {

    @Test
    void deepSeekIsDefaultAndSafeSummaryNeverContainsSecret() {
        AiModelProperties properties = new AiModelProperties();
        properties.setApiKey("a-very-secret-key");

        assertEquals(AiProvider.DEEPSEEK, properties.providerEnum());
        assertTrue(properties.safeSummary().contains("apiKeyConfigured=true"));
        assertFalse(properties.safeSummary().contains("a-very-secret-key"));
    }

    @Test
    void chatGptAliasMapsToOpenAi() {
        AiModelProperties properties = new AiModelProperties();
        properties.setProvider("chatgpt");
        assertEquals(AiProvider.OPENAI, properties.providerEnum());
    }
}
