package com.example.langchain4jmcpclient.assistant;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;

public final class SupportAssistantFactory {

    private SupportAssistantFactory() {
    }

    public static SupportAssistant create(ChatModel chatModel, ToolProvider toolProvider) {
        return AiServices.builder(SupportAssistant.class)
            .chatModel(chatModel)
            .toolProvider(toolProvider)
            .maxToolCallingRoundTrips(4)
            .build();
    }

    public static McpToolProvider toolProvider(McpClient... clients) {
        return McpToolProvider.builder()
            .mcpClients(clients)
            .failIfOneServerFails(true)
            .build();
    }
}
