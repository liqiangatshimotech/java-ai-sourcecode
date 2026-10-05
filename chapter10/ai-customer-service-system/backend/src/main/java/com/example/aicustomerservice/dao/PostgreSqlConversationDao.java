package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.ConversationChannel;
import com.example.aicustomerservice.entity.ConversationMessage;
import com.example.aicustomerservice.entity.ConversationPriority;
import com.example.aicustomerservice.entity.ConversationStatus;
import com.example.aicustomerservice.entity.CustomerTier;
import com.example.aicustomerservice.entity.MessageSenderType;
import com.example.aicustomerservice.entity.MessageVisibility;
import com.example.aicustomerservice.entity.SupportConversation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * PostgreSQL 会话 DAO。
 *
 * 主表只保存列表和状态机需要的字段，消息表按会话独立查询。这样列表接口不会产生消息表的笛卡尔积，
 * 打开某个会话详情时才加载聊天历史。
 */
public class PostgreSqlConversationDao implements ConversationDao {

    private static final String TAG_SEPARATOR = "|";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public PostgreSqlConversationDao(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public SupportConversation save(SupportConversation conversation) {
        upsertConversation(conversation);
        return conversation;
    }

    @Override
    public Optional<SupportConversation> findByConversationId(String conversationId) {
        List<SupportConversation> rows = jdbcTemplate.query(
            "SELECT * FROM cs_conversation WHERE conversation_id = ?",
            (rs, rowNum) -> mapConversation(rs),
            conversationId
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<SupportConversation> search(ConversationQuery query) {
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM cs_conversation WHERE 1 = 1");

        if (query.status() != null) {
            sql.append(" AND status = ?");
            parameters.add(query.status().name());
        }
        if (query.channel() != null) {
            sql.append(" AND channel = ?");
            parameters.add(query.channel().name());
        }
        if (notBlank(query.assignee())) {
            sql.append(" AND LOWER(assignee) = LOWER(?)");
            parameters.add(query.assignee().trim());
        }
        if (notBlank(query.queueName())) {
            sql.append(" AND LOWER(queue_name) = LOWER(?)");
            parameters.add(query.queueName().trim());
        }
        if (notBlank(query.keyword())) {
            sql.append("""
                 AND (
                    conversation_id ILIKE ?
                    OR customer_id ILIKE ?
                    OR customer_name ILIKE ?
                    OR subject ILIKE ?
                    OR order_no ILIKE ?
                    OR last_message_preview ILIKE ?
                    OR tags ILIKE ?
                 )
                """);
            String keyword = "%" + query.keyword().trim() + "%";
            for (int i = 0; i < 7; i++) {
                parameters.add(keyword);
            }
        }

        sql.append(" ORDER BY CASE WHEN status = 'RESOLVED' THEN 1 ELSE 0 END, last_message_at DESC LIMIT 200");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapConversation(rs), parameters.toArray());
    }

    @Override
    public ConversationMessage appendMessage(ConversationMessage message) {
        insertMessage(message);
        return message;
    }

    @Override
    public List<ConversationMessage> findMessages(String conversationId) {
        return jdbcTemplate.query(
            """
                SELECT message_id, conversation_id, sender_type, sender, visibility, content, created_at
                FROM cs_conversation_message
                WHERE conversation_id = ?
                ORDER BY created_at ASC, message_id ASC
                """,
            (rs, rowNum) -> mapMessage(rs),
            conversationId
        );
    }

    @Override
    public SupportConversation saveWithMessage(
        SupportConversation conversation,
        ConversationMessage message
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            upsertConversation(conversation);
            insertMessage(message);
        });
        return conversation;
    }

    private void upsertConversation(SupportConversation conversation) {
        jdbcTemplate.update(
            """
                INSERT INTO cs_conversation (
                    conversation_id, customer_id, customer_name, customer_tier, channel,
                    status, priority, subject, intent, order_no, queue_name, assignee,
                    tags, unread_count, last_message_preview, last_message_at,
                    created_at, updated_at, first_response_at, resolved_at, resolution
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (conversation_id) DO UPDATE SET
                    customer_id = EXCLUDED.customer_id,
                    customer_name = EXCLUDED.customer_name,
                    customer_tier = EXCLUDED.customer_tier,
                    channel = EXCLUDED.channel,
                    status = EXCLUDED.status,
                    priority = EXCLUDED.priority,
                    subject = EXCLUDED.subject,
                    intent = EXCLUDED.intent,
                    order_no = EXCLUDED.order_no,
                    queue_name = EXCLUDED.queue_name,
                    assignee = EXCLUDED.assignee,
                    tags = EXCLUDED.tags,
                    unread_count = EXCLUDED.unread_count,
                    last_message_preview = EXCLUDED.last_message_preview,
                    last_message_at = EXCLUDED.last_message_at,
                    updated_at = EXCLUDED.updated_at,
                    first_response_at = EXCLUDED.first_response_at,
                    resolved_at = EXCLUDED.resolved_at,
                    resolution = EXCLUDED.resolution
                """,
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
            serializeTags(conversation.tags()),
            conversation.unreadCount(),
            conversation.lastMessagePreview(),
            timestamp(conversation.lastMessageAt()),
            timestamp(conversation.createdAt()),
            timestamp(conversation.updatedAt()),
            timestamp(conversation.firstResponseAt()),
            timestamp(conversation.resolvedAt()),
            conversation.resolution()
        );
    }

    private void insertMessage(ConversationMessage message) {
        jdbcTemplate.update(
            """
                INSERT INTO cs_conversation_message (
                    message_id, conversation_id, sender_type, sender, visibility, content, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (message_id) DO NOTHING
                """,
            message.messageId(),
            message.conversationId(),
            message.senderType().name(),
            message.sender(),
            message.visibility().name(),
            message.content(),
            timestamp(message.createdAt())
        );
    }

    private SupportConversation mapConversation(ResultSet rs) throws SQLException {
        return new SupportConversation(
            rs.getString("conversation_id"),
            rs.getString("customer_id"),
            rs.getString("customer_name"),
            CustomerTier.valueOf(rs.getString("customer_tier")),
            ConversationChannel.valueOf(rs.getString("channel")),
            ConversationStatus.valueOf(rs.getString("status")),
            ConversationPriority.valueOf(rs.getString("priority")),
            rs.getString("subject"),
            rs.getString("intent"),
            rs.getString("order_no"),
            rs.getString("queue_name"),
            rs.getString("assignee"),
            deserializeTags(rs.getString("tags")),
            rs.getInt("unread_count"),
            rs.getString("last_message_preview"),
            instant(rs, "last_message_at"),
            instant(rs, "created_at"),
            instant(rs, "updated_at"),
            instant(rs, "first_response_at"),
            instant(rs, "resolved_at"),
            rs.getString("resolution")
        );
    }

    private ConversationMessage mapMessage(ResultSet rs) throws SQLException {
        return new ConversationMessage(
            rs.getString("message_id"),
            rs.getString("conversation_id"),
            MessageSenderType.valueOf(rs.getString("sender_type")),
            rs.getString("sender"),
            MessageVisibility.valueOf(rs.getString("visibility")),
            rs.getString("content"),
            instant(rs, "created_at")
        );
    }

    private String serializeTags(List<String> tags) {
        return String.join(TAG_SEPARATOR, tags);
    }

    private List<String> deserializeTags(String tags) {
        if (!notBlank(tags)) {
            return List.of();
        }
        return Arrays.stream(tags.split("\\|"))
            .map(String::trim)
            .filter(this::notBlank)
            .toList();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private Instant instant(ResultSet rs, String columnName) throws SQLException {
        Timestamp value = rs.getTimestamp(columnName);
        return value == null ? null : value.toInstant();
    }
}
