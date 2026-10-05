package com.example.langchain4jticketclassifier.assistant;

import com.example.langchain4jticketclassifier.dto.TicketClassification;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface TicketClassifierAssistant {

    @SystemMessage("""
        你是 SaaS 客服系统的工单分类器。
        只根据用户输入分类，不要编造不存在的事实。
        category 只能是 BILLING、BUG、ACCOUNT、FEATURE_REQUEST、OTHER。
        priority 只能是 LOW、MEDIUM、HIGH、URGENT。
        summary 必须控制在 80 个字符以内。
        requiredData 写继续处理前缺失的关键信息。
        confidence 取 0 到 1 的小数。
        """)
    @UserMessage("""
        请分类下面的工单：
        {{ticketText}}
        """)
    TicketClassification classify(@V("ticketText") String ticketText);
}
