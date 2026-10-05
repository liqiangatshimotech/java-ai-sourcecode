package com.example.springairag.service;

import com.example.springairag.rag.RagPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.rag.ai-mode", havingValue = "spring-ai")
public class SpringAiGateway implements AiGateway {

    private final ChatClient chatClient;

    public SpringAiGateway(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String answer(RagPrompt prompt) {
        return chatClient.prompt()
            .system(prompt.system())
            .user(prompt.user())
            .call()
            .content();
    }
}
