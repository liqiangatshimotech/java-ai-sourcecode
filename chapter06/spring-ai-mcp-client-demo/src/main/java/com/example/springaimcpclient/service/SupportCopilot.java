package com.example.springaimcpclient.service;

import org.springframework.stereotype.Service;

@Service
public class SupportCopilot {

    private final ChatClientRegistry registry;

    public SupportCopilot(ChatClientRegistry registry) {
        this.registry = registry;
    }

    public String answer(String provider, String question) {
        // MCP 工具已经在 AiModelConfiguration 里通过 defaultToolCallbacks 挂到每个 ChatClient 上，
        // 这里不能再调用 .toolCallbacks(...) 重复注册，否则 Spring AI 会因为工具重名直接报错。
        return registry.clientFor(provider)
            .prompt()
            .system("""
                你是售后支持助手。用户提到订单、退款、物流或政策时，必须先调用可用的 MCP 工具。
                如果工具没有返回足够信息，要明确说明还缺少什么，不要补写不存在的业务事实。
                """)
            .user(question)
            .call()
            .content();
    }
}
