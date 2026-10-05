package com.example.langchain4jchat.service;

import com.example.langchain4jchat.assistant.Assistant;
import com.example.langchain4jchat.assistant.StreamingAssistant;
import com.example.langchain4jchat.config.AiChatProperties;
import com.example.langchain4jchat.config.AiProvider;
import com.example.langchain4jchat.exception.ProviderNotConfiguredException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ChatClientRegistry {

    private final AiChatProperties properties;
    private final Map<AiProvider, ProviderClients> clients;

    public ChatClientRegistry(AiChatProperties properties) {
        this.properties = properties;
        this.clients = createClients(properties);
    }

    public AiProvider resolveProvider(String requestedProvider) {
        if (requestedProvider == null || requestedProvider.isBlank()) {
            return properties.getDefaultProvider();
        }
        return AiProvider.from(requestedProvider);
    }

    public ProviderClients clients(String requestedProvider) {
        AiProvider provider = resolveProvider(requestedProvider);
        ProviderClients providerClients = clients.get(provider);
        if (providerClients == null) {
            throw new ProviderNotConfiguredException("provider is not configured: " + provider.id());
        }
        return providerClients;
    }

    private Map<AiProvider, ProviderClients> createClients(AiChatProperties properties) {
        EnumMap<AiProvider, ProviderClients> registry = new EnumMap<>(AiProvider.class);
        registry.put(AiProvider.OLLAMA, createOllamaClients(properties));
        if (properties.getDeepseek().isConfigured()) {
            registry.put(AiProvider.DEEPSEEK, createCompatibleClients(properties, properties.getDeepseek()));
        }

        if (properties.getOpenai().isConfigured()) {
            registry.put(AiProvider.OPENAI, createOpenAiClients(properties));
        }

        return Map.copyOf(registry);
    }

    private ProviderClients createOllamaClients(AiChatProperties properties) {
        AiChatProperties.Ollama ollama = properties.getOllama();

        ChatModel chatModel = OllamaChatModel.builder()
                .baseUrl(ollama.getBaseUrl())
                .modelName(ollama.getModel())
                .temperature(ollama.getTemperature())
                .timeout(ollama.getTimeout())
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogResponses())
                .build();

        StreamingChatModel streamingChatModel = OllamaStreamingChatModel.builder()
                .baseUrl(ollama.getBaseUrl())
                .modelName(ollama.getModel())
                .temperature(ollama.getTemperature())
                .timeout(ollama.getTimeout())
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogResponses())
                .build();

        return createProviderClients(chatModel, streamingChatModel);
    }

    private ProviderClients createOpenAiClients(AiChatProperties properties) {
        return createCompatibleClients(properties, properties.getOpenai());
    }

    /** 同步、流式与 AI Services 共享同一供应商配置。 */
    private ProviderClients createCompatibleClients(AiChatProperties properties, AiChatProperties.OpenAi openai) {

        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(openai.getBaseUrl())
                .apiKey(openai.getApiKey())
                .modelName(openai.getModel())
                .temperature(openai.getTemperature())
                .timeout(openai.getTimeout())
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogResponses())
                .build();

        StreamingChatModel streamingChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(openai.getBaseUrl())
                .apiKey(openai.getApiKey())
                .modelName(openai.getModel())
                .temperature(openai.getTemperature())
                .timeout(openai.getTimeout())
                .logRequests(properties.isLogRequests())
                .logResponses(properties.isLogResponses())
                .build();

        return createProviderClients(chatModel, streamingChatModel);
    }

    private ProviderClients createProviderClients(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .build();

        StreamingAssistant streamingAssistant = AiServices.builder(StreamingAssistant.class)
                .streamingChatModel(streamingChatModel)
                .build();

        return new ProviderClients(chatModel, streamingChatModel, assistant, streamingAssistant);
    }
}
