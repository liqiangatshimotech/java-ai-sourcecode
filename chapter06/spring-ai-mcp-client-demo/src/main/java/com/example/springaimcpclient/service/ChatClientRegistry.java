package com.example.springaimcpclient.service;

import com.example.springaimcpclient.config.AiProvider;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;

public class ChatClientRegistry {

    private final Map<AiProvider, ChatClient> clients;

    private final AiProvider defaultProvider;

    public ChatClientRegistry(Map<AiProvider, ChatClient> clients, AiProvider defaultProvider) {
        this.clients = Map.copyOf(clients);
        this.defaultProvider = defaultProvider;
    }

    public ChatClient clientFor(String providerName) {
        AiProvider provider = AiProvider.parseOrDefault(providerName, defaultProvider);
        ChatClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalStateException("AI provider is not configured: " + provider.name().toLowerCase());
        }
        return client;
    }

    public Set<AiProvider> configuredProviders() {
        return clients.keySet();
    }
}
