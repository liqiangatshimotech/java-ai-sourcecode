package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.ConversationDetail;
import java.time.Instant;
import java.util.List;

/**
 * 会话详情响应。顶层是工作流字段，customer、tags、messages 可直接驱动客服工作台三栏布局。
 */
public record ConversationDetailResponse(
    String conversationId,
    String channel,
    String status,
    String priority,
    String subject,
    String intent,
    String orderNo,
    String queueName,
    String assignee,
    int unreadCount,
    String lastMessagePreview,
    Instant lastMessageAt,
    Instant createdAt,
    Instant updatedAt,
    Instant firstResponseAt,
    Instant resolvedAt,
    String resolution,
    ConversationCustomerResponse customer,
    List<String> tags,
    List<ConversationMessageResponse> messages
) {
    public static ConversationDetailResponse from(ConversationDetail detail) {
        var conversation = detail.conversation();
        return new ConversationDetailResponse(
            conversation.conversationId(),
            conversation.channel().name(),
            conversation.status().name(),
            conversation.priority().name(),
            conversation.subject(),
            conversation.intent(),
            conversation.orderNo(),
            conversation.queueName(),
            conversation.assignee(),
            conversation.unreadCount(),
            conversation.lastMessagePreview(),
            conversation.lastMessageAt(),
            conversation.createdAt(),
            conversation.updatedAt(),
            conversation.firstResponseAt(),
            conversation.resolvedAt(),
            conversation.resolution(),
            ConversationCustomerResponse.from(detail.customer()),
            conversation.tags(),
            detail.messages().stream().map(ConversationMessageResponse::from).toList()
        );
    }
}
