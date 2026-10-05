package com.example.langchain4jmcpclient.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SupportAssistant {

    @SystemMessage("""
        你是电商售后助手。
        涉及订单状态或退款建议时，必须调用 MCP 工具读取事实，不要编造订单数据。
        退款、赔付和跨租户数据都要遵守工具返回结果，不能承诺未授权处理动作。
        """)
    @UserMessage("""
        请回答用户问题：
        {{question}}
        """)
    String answer(@V("question") String question);
}
