package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.ConversationChannel;
import com.example.aicustomerservice.entity.ConversationStatus;

/** 客服收件箱查询条件，所有字段都可以为空。 */
public record ConversationQuery(
    ConversationStatus status,
    ConversationChannel channel,
    String assignee,
    String queueName,
    String keyword
) {
}
