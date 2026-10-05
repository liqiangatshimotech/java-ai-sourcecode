package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.ConversationChannel;
import com.example.aicustomerservice.entity.ConversationMessage;
import com.example.aicustomerservice.entity.ConversationPriority;
import com.example.aicustomerservice.entity.ConversationStatus;
import com.example.aicustomerservice.entity.CustomerTier;
import com.example.aicustomerservice.entity.MessageSenderType;
import com.example.aicustomerservice.entity.MessageVisibility;
import com.example.aicustomerservice.entity.SupportConversation;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话领域的内存存储。
 *
 * 该实现不是空壳：它包含和 PostgreSQL 模式一致的演示会话、消息、筛选和幂等行为，适合没有
 * Docker 的环境启动课程项目。数据只在当前进程生命周期内有效。
 */
public class InMemoryConversationDao implements ConversationDao {

    private final Map<String, SupportConversation> conversations = new ConcurrentHashMap<>();
    private final Map<String, ConversationMessage> messages = new ConcurrentHashMap<>();

    public InMemoryConversationDao(Clock clock) {
        this(clock, true);
    }

    /** 测试可以关闭种子数据，从一个完全空的收件箱开始构造场景。 */
    public InMemoryConversationDao(Clock clock, boolean seedData) {
        if (seedData) {
            seed(clock.instant());
        }
    }

    @Override
    public synchronized SupportConversation save(SupportConversation conversation) {
        conversations.put(conversation.conversationId(), conversation);
        return conversation;
    }

    @Override
    public Optional<SupportConversation> findByConversationId(String conversationId) {
        return Optional.ofNullable(conversations.get(conversationId));
    }

    @Override
    public List<SupportConversation> search(ConversationQuery query) {
        String assignee = normalize(query.assignee());
        String queueName = normalize(query.queueName());
        String keyword = normalize(query.keyword()).toLowerCase(Locale.ROOT);
        return conversations.values().stream()
            .filter(item -> query.status() == null || item.status() == query.status())
            .filter(item -> query.channel() == null || item.channel() == query.channel())
            .filter(item -> assignee.isBlank() || item.assignee().equalsIgnoreCase(assignee))
            .filter(item -> queueName.isBlank() || item.queueName().equalsIgnoreCase(queueName))
            .filter(item -> keyword.isBlank() || searchableText(item).contains(keyword))
            .sorted(Comparator
                .comparing((SupportConversation item) -> item.status() == ConversationStatus.RESOLVED)
                .thenComparing(SupportConversation::lastMessageAt, Comparator.reverseOrder()))
            .limit(200)
            .toList();
    }

    @Override
    public synchronized ConversationMessage appendMessage(ConversationMessage message) {
        ConversationMessage existing = messages.putIfAbsent(message.messageId(), message);
        return existing == null ? message : existing;
    }

    @Override
    public List<ConversationMessage> findMessages(String conversationId) {
        return messages.values().stream()
            .filter(message -> message.conversationId().equals(conversationId))
            .sorted(Comparator.comparing(ConversationMessage::createdAt).thenComparing(ConversationMessage::messageId))
            .toList();
    }

    @Override
    public synchronized SupportConversation saveWithMessage(
        SupportConversation conversation,
        ConversationMessage message
    ) {
        // 同一个 synchronized 临界区内完成两个 Map 的更新，内存模式也不会出现半成功状态。
        save(conversation);
        appendMessage(message);
        return conversation;
    }

    private String searchableText(SupportConversation item) {
        return String.join(" ",
            item.conversationId(),
            item.customerId(),
            item.customerName(),
            item.subject(),
            item.intent(),
            item.orderNo(),
            item.lastMessagePreview(),
            String.join(" ", item.tags())
        ).toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void seed(Instant now) {
        List<SupportConversation> seedConversations = List.of(
            conversation(
                "CONV-20260710-001", "C10001", "北京朝阳门店", CustomerTier.ENTERPRISE,
                ConversationChannel.WECHAT, ConversationStatus.WAITING, ConversationPriority.URGENT,
                "高价值订单取消与仓库拦截", "REFUND", "SO202606210004", "售后二线", "",
                List.of("企业客户", "高价值订单", "退款"), 2,
                "活动物料还没收到，现在可以取消并退款吗？", now.minusSeconds(18 * 60),
                now.minusSeconds(35 * 60), null, null, ""
            ),
            conversation(
                "CONV-20260710-002", "C10002", "华东 VIP 客户", CustomerTier.VIP,
                ConversationChannel.APP, ConversationStatus.IN_PROGRESS, ConversationPriority.HIGH,
                "物流轨迹长时间未更新", "LOGISTICS_QUERY", "SO202606210002", "物流专席", "张敏",
                List.of("VIP", "物流异常"), 0,
                "我正在为您核对承运商的最新回传。", now.minusSeconds(5 * 60),
                now.minusSeconds(52 * 60), now.minusSeconds(18 * 60), null, ""
            ),
            conversation(
                "CONV-20260710-003", "C10003", "普通注册用户", CustomerTier.NORMAL,
                ConversationChannel.WEB, ConversationStatus.WAITING_CUSTOMER, ConversationPriority.NORMAL,
                "电子发票抬头修改", "INVOICE", "SO202606210003", "综合服务", "李杰",
                List.of("发票", "等待资料"), 0,
                "请补充正确的企业抬头和税号，我们会继续为您处理。", now.minusSeconds(22 * 60),
                now.minusSeconds(95 * 60), now.minusSeconds(40 * 60), null, ""
            ),
            conversation(
                "CONV-20260710-004", "C10001", "北京朝阳门店", CustomerTier.ENTERPRISE,
                ConversationChannel.EMAIL, ConversationStatus.WAITING_INTERNAL, ConversationPriority.HIGH,
                "企业订单退款进度", "REFUND", "SO202606210004", "售后二线", "王芳",
                List.of("企业客户", "等待财务"), 0,
                "财务正在复核退款账户，预计今天 16:00 前返回结果。", now.minusSeconds(45 * 60),
                now.minusSeconds(4 * 3600), now.minusSeconds(3 * 3600), null, ""
            ),
            conversation(
                "CONV-20260710-005", "C10003", "普通注册用户", CustomerTier.NORMAL,
                ConversationChannel.WEB, ConversationStatus.RESOLVED, ConversationPriority.LOW,
                "会员积分到账时间", "FAQ", "", "综合服务", "李杰",
                List.of("FAQ", "已解决"), 0,
                "积分已经补发到账，您可以在会员中心查看。", now.minusSeconds(20 * 3600),
                now.minusSeconds(24 * 3600), now.minusSeconds(23 * 3600), now.minusSeconds(20 * 3600),
                "确认积分补发成功，客户无需继续处理。"
            )
        );
        seedConversations.forEach(this::save);

        List<ConversationMessage> seedMessages = new ArrayList<>();
        seedMessages.add(message("MSG-SEED-001", "CONV-20260710-001", MessageSenderType.CUSTOMER,
            "北京朝阳门店", MessageVisibility.CUSTOMER, "活动物料还没收到，现在可以取消并退款吗？", now.minusSeconds(18 * 60)));
        seedMessages.add(message("MSG-SEED-002", "CONV-20260710-001", MessageSenderType.AI,
            "ai-copilot", MessageVisibility.INTERNAL, "订单金额 5600 元且处于退款复核，需要人工确认仓库拦截结果后再承诺退款。", now.minusSeconds(17 * 60)));

        seedMessages.add(message("MSG-SEED-003", "CONV-20260710-002", MessageSenderType.CUSTOMER,
            "华东 VIP 客户", MessageVisibility.CUSTOMER, "物流两天没有更新，礼盒现在到哪里了？", now.minusSeconds(32 * 60)));
        seedMessages.add(message("MSG-SEED-004", "CONV-20260710-002", MessageSenderType.SYSTEM,
            "system", MessageVisibility.INTERNAL, "张敏领取了会话。", now.minusSeconds(19 * 60)));
        seedMessages.add(message("MSG-SEED-005", "CONV-20260710-002", MessageSenderType.AGENT,
            "张敏", MessageVisibility.CUSTOMER, "我正在为您核对承运商的最新回传。", now.minusSeconds(5 * 60)));

        seedMessages.add(message("MSG-SEED-006", "CONV-20260710-003", MessageSenderType.CUSTOMER,
            "普通注册用户", MessageVisibility.CUSTOMER, "发票抬头写错了，可以改成公司名称吗？", now.minusSeconds(70 * 60)));
        seedMessages.add(message("MSG-SEED-007", "CONV-20260710-003", MessageSenderType.AGENT,
            "李杰", MessageVisibility.CUSTOMER, "请补充正确的企业抬头和税号，我们会继续为您处理。", now.minusSeconds(22 * 60)));
        seedMessages.add(message("MSG-SEED-008", "CONV-20260710-003", MessageSenderType.SYSTEM,
            "李杰", MessageVisibility.INTERNAL, "会话已设为等待客户：等待开票资料。", now.minusSeconds(21 * 60)));

        seedMessages.add(message("MSG-SEED-009", "CONV-20260710-004", MessageSenderType.CUSTOMER,
            "北京朝阳门店", MessageVisibility.CUSTOMER, "退款申请昨天提交了，什么时候能到账？", now.minusSeconds(3 * 3600)));
        seedMessages.add(message("MSG-SEED-010", "CONV-20260710-004", MessageSenderType.AGENT,
            "王芳", MessageVisibility.INTERNAL, "已提交财务复核，关联订单 SO202606210004。", now.minusSeconds(2 * 3600)));
        seedMessages.add(message("MSG-SEED-011", "CONV-20260710-004", MessageSenderType.AGENT,
            "王芳", MessageVisibility.CUSTOMER, "财务正在复核退款账户，预计今天 16:00 前返回结果。", now.minusSeconds(45 * 60)));

        seedMessages.add(message("MSG-SEED-012", "CONV-20260710-005", MessageSenderType.CUSTOMER,
            "普通注册用户", MessageVisibility.CUSTOMER, "昨天的积分怎么还没到账？", now.minusSeconds(23 * 3600)));
        seedMessages.add(message("MSG-SEED-013", "CONV-20260710-005", MessageSenderType.AGENT,
            "李杰", MessageVisibility.CUSTOMER, "积分已经补发到账，您可以在会员中心查看。", now.minusSeconds(20 * 3600)));
        seedMessages.add(message("MSG-SEED-014", "CONV-20260710-005", MessageSenderType.SYSTEM,
            "李杰", MessageVisibility.INTERNAL, "会话已解决：确认积分补发成功，客户无需继续处理。", now.minusSeconds(20 * 3600 - 30)));
        seedMessages.forEach(this::appendMessage);
    }

    private SupportConversation conversation(
        String id,
        String customerId,
        String customerName,
        CustomerTier tier,
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
        String preview,
        Instant lastMessageAt,
        Instant createdAt,
        Instant firstResponseAt,
        Instant resolvedAt,
        String resolution
    ) {
        return new SupportConversation(
            id,
            customerId,
            customerName,
            tier,
            channel,
            status,
            priority,
            subject,
            intent,
            orderNo,
            queueName,
            assignee,
            tags,
            unreadCount,
            preview,
            lastMessageAt,
            createdAt,
            lastMessageAt,
            firstResponseAt,
            resolvedAt,
            resolution
        );
    }

    private ConversationMessage message(
        String messageId,
        String conversationId,
        MessageSenderType senderType,
        String sender,
        MessageVisibility visibility,
        String content,
        Instant createdAt
    ) {
        return new ConversationMessage(messageId, conversationId, senderType, sender, visibility, content, createdAt);
    }
}
