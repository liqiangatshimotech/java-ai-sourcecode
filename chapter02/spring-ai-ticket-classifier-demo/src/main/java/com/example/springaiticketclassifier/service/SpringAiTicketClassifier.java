package com.example.springaiticketclassifier.service;

import com.example.springaiticketclassifier.dto.TicketClassification;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SpringAiTicketClassifier {

    private final ChatClient chatClient;
    private final TicketClassificationValidator validator;

    public SpringAiTicketClassifier(
        ChatClient.Builder builder,
        TicketClassificationValidator validator
    ) {
        this.chatClient = builder.build();
        this.validator = validator;
    }

    public String classifyAsText(String ticketText) {
        return this.chatClient.prompt()
            .system("""
                你是 SaaS 客服系统的工单分类器。
                只根据用户输入分类，不要补充不存在的事实。
                """)
            .user(user -> user.text("""
                请分类下面的工单：
                {ticketText}
                """).param("ticketText", ticketText))
            .call()
            .content();
    }

    public TicketClassification classify(String ticketText) {
        TicketClassification result = this.chatClient.prompt()
            .system("""
                你是 SaaS 客服系统的工单分类器。
                只根据用户输入分类，不要编造不存在的事实。
                category 只能是 BILLING、BUG、ACCOUNT、FEATURE_REQUEST、OTHER。
                priority 只能是 LOW、MEDIUM、HIGH、URGENT。
                summary 必须控制在 80 个字符以内。
                requiredData 写继续处理前缺失的关键信息。
                confidence 取 0 到 1 的小数。
                """)
            .user(user -> user.text("""
                请分类下面的工单：
                {ticketText}
                """).param("ticketText", ticketText))
            .call()
            .entity(TicketClassification.class);

        this.validator.validate(result);
        return result;
    }

    public TicketClassification classifyWithConverter(String ticketText) {
        BeanOutputConverter<TicketClassification> converter =
            new BeanOutputConverter<>(TicketClassification.class);

        String content = this.chatClient.prompt()
            .system("你是 SaaS 客服系统的工单分类器，只输出符合格式要求的结果。")
            .user(user -> user.text("""
                请分类下面的工单：
                {ticketText}

                输出要求：
                {format}
                """).params(Map.of(
                    "ticketText", ticketText,
                    "format", converter.getFormat()
                )))
            .call()
            .content();

        TicketClassification result = converter.convert(content);
        this.validator.validate(result);
        return result;
    }
}
