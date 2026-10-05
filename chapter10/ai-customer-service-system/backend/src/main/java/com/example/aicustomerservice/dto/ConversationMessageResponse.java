package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.ConversationMessage;
import java.time.Instant;

/** 客服工作台展示的一条消息。visibility=INTERNAL 时前端必须使用内部备注样式。 */
public record ConversationMessageResponse(
    String messageId,
    String senderType,
    String sender,
    String visibility,
    String content,
    Instant createdAt
) {
    public static ConversationMessageResponse from(ConversationMessage message) {
        return new ConversationMessageResponse(
            message.messageId(),
            message.senderType().name(),
            message.sender(),
            message.visibility().name(),
            message.content(),
            message.createdAt()
        );
    }
}
