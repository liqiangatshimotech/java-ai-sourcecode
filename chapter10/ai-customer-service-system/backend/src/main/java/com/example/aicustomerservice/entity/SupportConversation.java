package com.example.aicustomerservice.entity;

import java.time.Instant;
import java.util.List;

/**
 * 客服收件箱中的会话主数据。
 *
 * 消息正文单独保存在 cs_conversation_message，列表页只读取这里的摘要字段，避免为了展示几十条
 * 会话而加载全部聊天历史。详情页再按 conversationId 查询完整消息。
 */
public record SupportConversation(
    String conversationId,
    String customerId,
    String customerName,
    CustomerTier customerTier,
    ConversationChannel channel,
    ConversationStatus status,
    ConversationPriority priority,
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
    String resolution
) {
    public SupportConversation {
        tags = tags == null ? List.of() : List.copyOf(tags);
        assignee = assignee == null ? "" : assignee;
        orderNo = orderNo == null ? "" : orderNo;
        intent = intent == null ? "UNKNOWN" : intent;
        resolution = resolution == null ? "" : resolution;
    }

    /** 领取或转接会话时更新处理人；只有首次进入处理态时才清空未读数。 */
    public SupportConversation assignedTo(String owner, ConversationStatus nextStatus, Instant now) {
        return copy(nextStatus, owner, 0, lastMessagePreview, lastMessageAt, now, firstResponseAt, resolvedAt, resolution);
    }

    /** 客服发送客户可见回复后，更新列表摘要并记录首次响应时间。 */
    public SupportConversation replied(String content, Instant now) {
        Instant responseAt = firstResponseAt == null ? now : firstResponseAt;
        return copy(status, assignee, 0, preview(content), now, now, responseAt, resolvedAt, resolution);
    }

    /** 等待客户或内部团队时只改变工作流状态，不改写最近一条客户可见消息。 */
    public SupportConversation waiting(ConversationStatus nextStatus, Instant now) {
        return copy(nextStatus, assignee, unreadCount, lastMessagePreview, lastMessageAt, now, firstResponseAt, null, "");
    }

    /** 解决会话后记录解决摘要和完成时间。 */
    public SupportConversation resolved(String summary, Instant now) {
        return copy(ConversationStatus.RESOLVED, assignee, 0, lastMessagePreview, lastMessageAt, now, firstResponseAt, now, summary);
    }

    private SupportConversation copy(
        ConversationStatus nextStatus,
        String nextAssignee,
        int nextUnreadCount,
        String nextPreview,
        Instant nextLastMessageAt,
        Instant nextUpdatedAt,
        Instant nextFirstResponseAt,
        Instant nextResolvedAt,
        String nextResolution
    ) {
        return new SupportConversation(
            conversationId,
            customerId,
            customerName,
            customerTier,
            channel,
            nextStatus,
            priority,
            subject,
            intent,
            orderNo,
            queueName,
            nextAssignee,
            tags,
            nextUnreadCount,
            nextPreview,
            nextLastMessageAt,
            createdAt,
            nextUpdatedAt,
            nextFirstResponseAt,
            nextResolvedAt,
            nextResolution
        );
    }

    private String preview(String content) {
        String normalized = content == null ? "" : content.strip().replaceAll("\\s+", " ");
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "…";
    }
}
