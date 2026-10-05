package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.SupportConversation;
import com.example.aicustomerservice.entity.ConversationStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** 收件箱列表项。字段保持扁平，前端渲染列表时不需要额外读取详情。 */
public record ConversationSummaryResponse(
    String conversationId,
    String customerId,
    String customerName,
    String customerTier,
    String channel,
    String status,
    String priority,
    String subject,
    String intent,
    String orderNo,
    String queueName,
    String assignee,
    List<String> tags,
    int unreadCount,
    String lastMessagePreview,
    Instant lastMessageAt,
    Instant createdAt,
    Instant updatedAt,
    Instant firstResponseAt,
    Instant resolvedAt,
    String resolution,
    long waitingMinutes
) {
    public static ConversationSummaryResponse from(SupportConversation conversation, Instant now) {
        long waitingMinutes = conversation.status() == ConversationStatus.WAITING
            ? Math.max(0, Duration.between(conversation.lastMessageAt(), now).toMinutes())
            : 0;
        return new ConversationSummaryResponse(
            conversation.conversationId(),
            conversation.customerId(),
            conversation.customerName(),
            conversation.customerTier().name(),
            conversation.channel().name(),
            conversation.status().name(),
            conversation.priority().name(),
            conversation.subject(),
            conversation.intent(),
            conversation.orderNo(),
            conversation.queueName(),
            conversation.assignee(),
            conversation.tags(),
            conversation.unreadCount(),
            conversation.lastMessagePreview(),
            conversation.lastMessageAt(),
            conversation.createdAt(),
            conversation.updatedAt(),
            conversation.firstResponseAt(),
            conversation.resolvedAt(),
            conversation.resolution(),
            waitingMinutes
        );
    }
}
