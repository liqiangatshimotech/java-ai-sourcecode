package com.example.langchain4jmcpclient.demo;

import com.example.langchain4jmcpclient.assistant.SupportAssistant;
import com.example.langchain4jmcpclient.assistant.SupportAssistantFactory;
import com.example.langchain4jmcpclient.config.AiModelProperties;
import com.example.langchain4jmcpclient.config.ChatModelFactory;
import com.example.langchain4jmcpclient.mcp.McpClientFactory;
import com.example.langchain4jmcpclient.mcp.McpServerConnectionProperties;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.model.chat.ChatModel;

public final class LangChain4jMcpClientDemo {

    private LangChain4jMcpClientDemo() {
    }

    public static void main(String[] args) throws Exception {
        String question = args.length == 0
            ? "请查询 ORD-1001 的订单状态，并判断是否可以退款。"
            : String.join(" ", args);

        AiModelProperties aiProperties = AiModelProperties.fromEnv();
        ChatModel chatModel = ChatModelFactory.createDefault(aiProperties);
        McpServerConnectionProperties mcpProperties = McpServerConnectionProperties.demoStdioCommand();

        try (McpClient mcpClient = McpClientFactory.stdio(mcpProperties)) {
            SupportAssistant assistant = SupportAssistantFactory.create(
                chatModel,
                SupportAssistantFactory.toolProvider(mcpClient)
            );
            System.out.println(assistant.answer(question));
        }
    }
}
