package com.example.aicustomerservice.entity;

import java.time.Instant;

/**
 * 一条持久化的客服消息。
 *
 * messageId 是业务幂等键，渠道重复投递同一条消息时可以复用该 ID，避免聊天记录重复。
 */
public record ConversationMessage(
    String messageId,
    String conversationId,
    MessageSenderType senderType,
    String sender,
    MessageVisibility visibility,
    String content,
    Instant createdAt
) {
}
