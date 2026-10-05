package com.example.langchain4jchat.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AiProviderTest {

    @Test
    void resolvesProviderIgnoringCaseAndWhitespace() {
        assertThat(AiProvider.from(" ollama ")).isEqualTo(AiProvider.OLLAMA);
        assertThat(AiProvider.from("OPENAI")).isEqualTo(AiProvider.OPENAI);
    }

    @Test
    void rejectsUnknownProvider() {
        assertThatThrownBy(() -> AiProvider.from("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported provider");
    }
}
