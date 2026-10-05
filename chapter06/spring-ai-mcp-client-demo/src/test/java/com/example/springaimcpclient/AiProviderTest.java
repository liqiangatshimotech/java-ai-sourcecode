package com.example.springaimcpclient;

import com.example.springaimcpclient.config.AiProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiProviderTest {

    @Test
    void parsesConfiguredProviders() {
        assertEquals(AiProvider.DEEPSEEK, AiProvider.parse("deepseek"));
        assertEquals(AiProvider.OLLAMA, AiProvider.parse("ollama"));
        assertEquals(AiProvider.CHATGPT, AiProvider.parse("chatgpt"));
    }

    @Test
    void usesDefaultProviderForBlankValue() {
        assertEquals(AiProvider.DEEPSEEK, AiProvider.parseOrDefault("", AiProvider.DEEPSEEK));
    }

    @Test
    void rejectsUnsupportedProvider() {
        assertThrows(IllegalArgumentException.class, () -> AiProvider.parse("claude"));
    }
}
