package com.example.existingservicemcpclient;

import com.example.existingservicemcpclient.config.AiProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiProviderTest {

    @Test
    void parsesSupportedProviders() {
        assertEquals(AiProvider.DEEPSEEK, AiProvider.parse("deepseek"));
        assertEquals(AiProvider.OLLAMA, AiProvider.parse("ollama"));
        assertEquals(AiProvider.CHATGPT, AiProvider.parse("chatgpt"));
    }

    @Test
    void rejectsUnsupportedProvider() {
        assertThrows(IllegalArgumentException.class, () -> AiProvider.parse("gemini"));
    }
}
