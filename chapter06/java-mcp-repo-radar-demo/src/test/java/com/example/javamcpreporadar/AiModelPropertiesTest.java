package com.example.javamcpreporadar;

import com.example.javamcpreporadar.config.AiModelProperties;
import com.example.javamcpreporadar.config.AiProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiModelPropertiesTest {

    @Test
    void defaultsToDeepSeek() {
        AiModelProperties properties = AiModelProperties.fromEnv(Map.of());

        assertThat(properties.defaultProvider()).isEqualTo(AiProvider.DEEPSEEK);
        assertThat(properties.deepseek().baseUrl()).isEqualTo("https://api.deepseek.com/v1");
        assertThat(properties.deepseek().model()).isEqualTo("deepseek-v4-flash");
    }

    @Test
    void canSwitchToOllama() {
        AiModelProperties properties = AiModelProperties.fromEnv(Map.of("APP_AI_DEFAULT_PROVIDER", "ollama"));

        assertThat(properties.defaultProvider()).isEqualTo(AiProvider.OLLAMA);
        assertThat(properties.defaultProviderConfigured()).isTrue();
    }
}
