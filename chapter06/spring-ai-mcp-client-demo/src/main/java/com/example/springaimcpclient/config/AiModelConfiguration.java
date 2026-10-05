package com.example.springaimcpclient.config;

import com.example.springaimcpclient.service.ChatClientRegistry;
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
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiModelConfiguration.class);

    @Bean
    ChatClientRegistry chatClientRegistry(
        AiClientProperties properties,
        ObjectProvider<ToolCallbackProvider> mcpToolCallbacksProvider
    ) {
        ToolCallbackProvider mcpToolCallbacks = mcpToolCallbacksProvider.getIfAvailable(ToolCallbackProvider::from);
        Map<AiProvider, ChatClient> clients = new EnumMap<>(AiProvider.class);

        if (properties.getDeepseek().isConfigured()) {
            clients.put(AiProvider.DEEPSEEK, buildOpenAiCompatibleClient(properties, properties.getDeepseek(), mcpToolCallbacks));
        }
        else {
            log.info("DeepSeek provider is disabled because DEEPSEEK_API_KEY is empty.");
        }

        clients.put(AiProvider.OLLAMA, buildOllamaClient(properties, mcpToolCallbacks));

        if (properties.getChatgpt().isConfigured()) {
            clients.put(AiProvider.CHATGPT, buildOpenAiCompatibleClient(properties, properties.getChatgpt(), mcpToolCallbacks));
        }
        else {
            log.info("ChatGPT provider is disabled because CHATGPT_API_KEY is empty.");
        }

        return new ChatClientRegistry(clients, properties.getDefaultProvider());
    }

    private ChatClient buildOpenAiCompatibleClient(
        AiClientProperties properties,
        AiClientProperties.OpenAiCompatible provider,
        ToolCallbackProvider mcpToolCallbacks
    ) {
        OpenAiApi api = OpenAiApi.builder()
            .baseUrl(provider.getBaseUrl())
            .apiKey(provider.getApiKey())
            .build();

        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
            .model(provider.getModel())
            .temperature(provider.getTemperature());

        if (provider.getMaxTokens() != null && provider.getMaxTokens() > 0) {
            options.maxTokens(provider.getMaxTokens());
        }

        OpenAiChatModel model = OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(options.build())
            .build();

        return ChatClient.builder(model)
            .defaultSystem(properties.getSystemPrompt())
            .defaultToolCallbacks(mcpToolCallbacks)
            .build();
    }

    private ChatClient buildOllamaClient(AiClientProperties properties, ToolCallbackProvider mcpToolCallbacks) {
        AiClientProperties.Ollama ollama = properties.getOllama();

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
            .defaultToolCallbacks(mcpToolCallbacks)
            .build();
    }
}
