package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.ConversationMessage;
import com.example.aicustomerservice.entity.SupportConversation;
import java.util.List;
import java.util.Optional;

/**
 * 会话领域的存储边界。
 *
 * saveWithMessage 用于需要同时更新会话摘要和追加事件的业务动作。PostgreSQL 实现会在一个事务中完成，
 * 防止出现“状态已经改变但时间线没有记录”的半成功数据。
 */
public interface ConversationDao {

    SupportConversation save(SupportConversation conversation);

    Optional<SupportConversation> findByConversationId(String conversationId);

    List<SupportConversation> search(ConversationQuery query);

    ConversationMessage appendMessage(ConversationMessage message);

    List<ConversationMessage> findMessages(String conversationId);

    default SupportConversation saveWithMessage(
        SupportConversation conversation,
        ConversationMessage message
    ) {
        save(conversation);
        appendMessage(message);
        return conversation;
    }
}
