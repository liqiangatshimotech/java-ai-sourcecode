package com.example.aicustomerservice.entity;

import java.util.List;

/**
 * 会话详情聚合。主数据、客户信息和消息历史一次返回，直接满足客服工作台打开会话的需要。
 */
public record ConversationDetail(
    SupportConversation conversation,
    CustomerProfile customer,
    List<ConversationMessage> messages
) {
    public ConversationDetail {
        messages = List.copyOf(messages);
    }
}
