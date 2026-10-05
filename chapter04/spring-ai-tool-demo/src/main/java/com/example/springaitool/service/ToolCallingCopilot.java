package com.example.springaitool.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class ToolCallingCopilot {

    private final ChatClient chatClient;
    private final ToolCallbackProvider commerceToolCallbacks;

    public ToolCallingCopilot(ChatClient.Builder builder, ToolCallbackProvider commerceToolCallbacks) {
        this.chatClient = builder.build();
        this.commerceToolCallbacks = commerceToolCallbacks;
    }

    public String answer(String userQuestion) {
        return this.chatClient.prompt()
            .system("""
                你是电商客服助手。
                需要订单、库存或客户信息时，必须先调用工具，不要编造业务数据。
                写操作、高风险操作和退款承诺必须转人工确认。
                """)
            .user(userQuestion)
            // 这是 Spring AI Tool Calling 的关键：把工具回调挂到本次模型调用上。
            .toolCallbacks(this.commerceToolCallbacks)
            .call()
            .content();
    }
}
