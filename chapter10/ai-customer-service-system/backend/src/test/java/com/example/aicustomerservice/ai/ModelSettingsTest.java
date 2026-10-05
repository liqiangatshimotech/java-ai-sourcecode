package com.example.aicustomerservice.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelSettingsTest {

    @Test
    void shouldUseDeepSeekModelFromEnvironmentOrDefault() {
        ModelSettings settings = ModelSettings.fromProviderName("");

        assertThat(settings.provider()).isEqualTo(AiProvider.DEEPSEEK);
        assertThat(settings.modelName()).isNotBlank();
    }

    @Test
    void shouldSupportOllamaAndChatGptAlias() {
        assertThat(ModelSettings.fromProviderName("ollama").provider()).isEqualTo(AiProvider.OLLAMA);
        assertThat(ModelSettings.fromProviderName("chatgpt").provider()).isEqualTo(AiProvider.CHATGPT);
        assertThat(ModelSettings.fromProviderName("openai").provider()).isEqualTo(AiProvider.CHATGPT);
    }

    @Test
    void shouldRejectUnknownProvider() {
        assertThatThrownBy(() -> ModelSettings.fromProviderName("unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("不支持");
    }
}
