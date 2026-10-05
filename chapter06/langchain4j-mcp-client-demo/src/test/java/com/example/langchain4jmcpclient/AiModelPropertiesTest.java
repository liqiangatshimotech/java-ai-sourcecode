package com.example.langchain4jmcpclient;

import com.example.langchain4jmcpclient.config.AiModelProperties;
import com.example.langchain4jmcpclient.config.AiProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiModelPropertiesTest {

    @Test
    void defaultsToDeepseekAndKeepsOllamaAndChatgptConfig() {
        AiModelProperties properties = AiModelProperties.fromEnv(Map.of());

        assertThat(properties.defaultProvider()).isEqualTo(AiProvider.DEEPSEEK);
        assertThat(properties.deepseek().baseUrl()).isEqualTo("https://api.deepseek.com/v1");
        assertThat(properties.deepseek().model()).isEqualTo("deepseek-v4-flash");
        assertThat(properties.ollama().baseUrl()).isEqualTo("http://localhost:11434");
        assertThat(properties.chatgpt().model()).isEqualTo("gpt-4o-mini");
    }

    @Test
    void readsOpenAiKeyAsChatgptFallback() {
        AiModelProperties properties = AiModelProperties.fromEnv(Map.of("OPENAI_API_KEY", "sk-test"));

        assertThat(properties.chatgpt().apiKey()).isEqualTo("sk-test");
        assertThat(properties.chatgpt().configured()).isTrue();
    }
}
