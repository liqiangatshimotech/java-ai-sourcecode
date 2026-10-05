package com.example.springaichat.service;

import com.example.springaichat.config.AiProvider;
import com.example.springaichat.exception.ProviderNotConfiguredException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Keeps provider lookup in one place.
 *
 * <p>Later lessons can replace this simple registry with routing, fallback,
 * cost-aware model selection, or per-user provider permissions.</p>
 */
public class ChatClientRegistry {

    private final Map<AiProvider, ChatClient> clients;

    private final AiProvider defaultProvider;

    public ChatClientRegistry(Map<AiProvider, ChatClient> clients, AiProvider defaultProvider) {
        this.clients = Collections.unmodifiableMap(new EnumMap<>(clients));
        this.defaultProvider = defaultProvider;
    }

    public ChatClient clientFor(String providerName) {
        AiProvider provider = AiProvider.parseOrDefault(providerName, defaultProvider);
        ChatClient client = clients.get(provider);
        if (client == null) {
            throw new ProviderNotConfiguredException(provider);
        }
        return client;
    }

    public AiProvider providerFor(String providerName) {
        return AiProvider.parseOrDefault(providerName, defaultProvider);
    }

    public Set<AiProvider> availableProviders() {
        return clients.keySet();
    }
}
