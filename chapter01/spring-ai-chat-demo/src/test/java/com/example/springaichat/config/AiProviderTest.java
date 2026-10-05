package com.example.springaichat.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AiProviderTest {

    @Test
    void parseAcceptsLowercaseProviderName() {
        assertThat(AiProvider.parse("ollama")).isEqualTo(AiProvider.OLLAMA);
        assertThat(AiProvider.parse("openai")).isEqualTo(AiProvider.OPENAI);
    }

    @Test
    void parseOrDefaultUsesConfiguredDefaultForBlankInput() {
        assertThat(AiProvider.parseOrDefault("", AiProvider.OLLAMA)).isEqualTo(AiProvider.OLLAMA);
        assertThat(AiProvider.parseOrDefault(null, AiProvider.OPENAI)).isEqualTo(AiProvider.OPENAI);
    }

    @Test
    void parseRejectsUnsupportedProviderName() {
        assertThatThrownBy(() -> AiProvider.parse("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deepseek, ollama, openai or chatgpt");
    }
}
