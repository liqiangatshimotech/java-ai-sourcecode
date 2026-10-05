package com.example.springaichat.config;

import com.example.springaichat.service.ChatClientRegistry;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiModelConfiguration.class);

    /**
     * Create all ChatClient instances used by the application.
     *
     * <p>Ollama is always registered because it does not need a secret. OpenAI is registered
     * only when OPENAI_API_KEY is present, which prevents accidental startup failures and
     * avoids putting secrets in source code.</p>
     */
    @Bean
    ChatClientRegistry chatClientRegistry(AiChatProperties properties) {
        Map<AiProvider, ChatClient> clients = new EnumMap<>(AiProvider.class);

        clients.put(AiProvider.OLLAMA, buildOllamaClient(properties));
        if (properties.getDeepseek().isConfigured()) {
            clients.put(AiProvider.DEEPSEEK, buildCompatibleClient(properties.getDeepseek(), properties.getSystemPrompt()));
        }

        if (properties.getOpenai().isConfigured()) {
            clients.put(AiProvider.OPENAI, buildOpenAiClient(properties));
        }
        else {
            log.info("OpenAI provider is disabled because OPENAI_API_KEY is empty.");
        }

        return new ChatClientRegistry(clients, properties.getDefaultProvider());
    }

    private ChatClient buildOllamaClient(AiChatProperties properties) {
        AiChatProperties.Ollama ollama = properties.getOllama();

        OllamaApi api = OllamaApi.builder()
                .baseUrl(ollama.getBaseUrl())
                .build();

        OllamaChatModel model = OllamaChatModel.builder()
                .ollamaApi(api)
                .defaultOptions(OllamaChatOptions.builder()
                        .model(ollama.getModel())
                        .temperature(ollama.getTemperature())
                        .build())
                .build();

        return ChatClient.builder(model)
                .defaultSystem(properties.getSystemPrompt())
                .build();
    }

    private ChatClient buildOpenAiClient(AiChatProperties properties) {
        return buildCompatibleClient(properties.getOpenai(), properties.getSystemPrompt());
    }

    /** DeepSeek 与 OpenAI 复用协议，供应商配置仍然独立。 */
    private ChatClient buildCompatibleClient(AiChatProperties.OpenAi openai, String systemPrompt) {

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(openai.getBaseUrl())
                .apiKey(openai.getApiKey())
                .build();

        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
                .model(openai.getModel())
                .temperature(openai.getTemperature());

        if (openai.getMaxTokens() != null && openai.getMaxTokens() > 0) {
            options.maxTokens(openai.getMaxTokens());
        }

        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options.build())
                .build();

        return ChatClient.builder(model)
                .defaultSystem(systemPrompt)
                .build();
    }
}
