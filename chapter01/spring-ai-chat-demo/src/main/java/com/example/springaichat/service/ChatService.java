package com.example.springaichat.service;

import com.example.springaichat.config.AiProvider;
import com.example.springaichat.dto.ChatReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClientRegistry registry;

    public ChatService(ChatClientRegistry registry) {
        this.registry = registry;
    }

    /**
     * Synchronous chat call used by POST /chat.
     *
     * <p>The log intentionally records provider and message length only. Production systems
     * should avoid logging full user prompts unless auditing rules explicitly require it.</p>
     */
    public ChatReply chat(String message, String providerName) {
        AiProvider provider = registry.providerFor(providerName);
        ChatClient client = registry.clientFor(providerName);

        log.info("Calling AI provider={} mode=sync messageLength={}", provider, message.length());

        String content = client.prompt()
                .user(message)
                .call()
                .content();

        return new ChatReply(provider.name().toLowerCase(), content);
    }

    /**
     * Streaming chat call used by GET /chat/stream.
     */
    public Flux<String> stream(String message, String providerName) {
        AiProvider provider = registry.providerFor(providerName);
        ChatClient client = registry.clientFor(providerName);

        log.info("Calling AI provider={} mode=stream messageLength={}", provider, message.length());

        return client.prompt()
                .user(message)
                .stream()
                .content();
    }
}
