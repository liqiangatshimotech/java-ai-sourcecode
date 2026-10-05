package com.example.langchain4jtool.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CommerceAssistant {

    @SystemMessage("""
        你是电商客服助手。
        需要订单、库存或客户信息时，必须先调用工具，不要编造业务数据。
        写操作、高风险操作和退款承诺必须转人工确认。
        """)
    @UserMessage("""
        请回答用户问题：
        {{question}}
        """)
    String answer(@V("question") String question);
}
