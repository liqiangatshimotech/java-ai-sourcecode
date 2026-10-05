package com.example.langchain4jchat.service;

import com.example.langchain4jchat.assistant.Assistant;
import com.example.langchain4jchat.assistant.StreamingAssistant;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

public record ProviderClients(
        ChatModel chatModel,
        StreamingChatModel streamingChatModel,
        Assistant assistant,
        StreamingAssistant streamingAssistant
) {
}
