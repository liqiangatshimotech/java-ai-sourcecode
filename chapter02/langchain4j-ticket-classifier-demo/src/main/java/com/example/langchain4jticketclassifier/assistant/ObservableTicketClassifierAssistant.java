package com.example.langchain4jticketclassifier.assistant;

import com.example.langchain4jticketclassifier.dto.TicketClassification;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ObservableTicketClassifierAssistant {

    @SystemMessage("你是 SaaS 客服系统的工单分类器，只返回符合 Java 类型要求的结果。")
    @UserMessage("请分类下面的工单：{{ticketText}}")
    Result<TicketClassification> classifyWithMetadata(@V("ticketText") String ticketText);
}
