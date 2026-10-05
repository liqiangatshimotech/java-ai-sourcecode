package com.example.langchain4jmcpclient.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;

public final class ChatModelFactory {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private ChatModelFactory() {
    }

    public static ChatModel createDefault(AiModelProperties properties) {
        return create(properties.defaultProvider(), properties);
    }

    public static ChatModel create(AiProvider provider, AiModelProperties properties) {
        return switch (provider) {
            case DEEPSEEK -> openAiCompatible(properties.deepseek(), "DEEPSEEK_API_KEY");
            case CHATGPT -> openAiCompatible(properties.chatgpt(), "CHATGPT_API_KEY or OPENAI_API_KEY");
            case OLLAMA -> ollama(properties.ollama());
        };
    }

    private static ChatModel openAiCompatible(AiModelProperties.OpenAiCompatible properties, String apiKeyName) {
        if (!properties.configured()) {
            throw new IllegalStateException(apiKeyName + " must be configured before using this provider.");
        }
        return OpenAiChatModel.builder()
            .apiKey(properties.apiKey())
            .baseUrl(properties.baseUrl())
            .modelName(properties.model())
            .temperature(properties.temperature())
            .maxTokens(properties.maxTokens())
            .timeout(DEFAULT_TIMEOUT)
            .build();
    }

    private static ChatModel ollama(AiModelProperties.Ollama properties) {
        return OllamaChatModel.builder()
            .baseUrl(properties.baseUrl())
            .modelName(properties.model())
            .temperature(properties.temperature())
            .timeout(DEFAULT_TIMEOUT)
            .build();
    }
}
