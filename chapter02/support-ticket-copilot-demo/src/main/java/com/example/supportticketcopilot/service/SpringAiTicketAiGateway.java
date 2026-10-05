package com.example.supportticketcopilot.service;

import com.example.supportticketcopilot.dto.TicketAnalysis;
import com.example.supportticketcopilot.prompt.TicketPromptBuilder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class SpringAiTicketAiGateway implements TicketAiGateway {

    private final ChatClient chatClient;

    public SpringAiTicketAiGateway(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public TicketAnalysis analyzeTicket(String prompt) {
        return chatClient.prompt()
            .system(TicketPromptBuilder.ANALYSIS_SYSTEM)
            .user(prompt)
            .call()
            .entity(TicketAnalysis.class);
    }

    @Override
    public Flux<String> streamCustomerReply(String prompt) {
        return chatClient.prompt()
            .system(TicketPromptBuilder.REPLY_SYSTEM)
            .user(prompt)
            .stream()
            .content();
    }
}
