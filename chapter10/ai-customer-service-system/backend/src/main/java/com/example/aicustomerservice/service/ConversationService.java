package com.example.aicustomerservice.service;

import com.example.aicustomerservice.common.BusinessException;
import com.example.aicustomerservice.dao.ConversationDao;
import com.example.aicustomerservice.dao.ConversationQuery;
import com.example.aicustomerservice.dao.CustomerDao;
import com.example.aicustomerservice.entity.ConversationChannel;
import com.example.aicustomerservice.entity.ConversationDashboard;
import com.example.aicustomerservice.entity.ConversationDetail;
import com.example.aicustomerservice.entity.ConversationMessage;
import com.example.aicustomerservice.entity.ConversationPriority;
import com.example.aicustomerservice.entity.ConversationStatus;
import com.example.aicustomerservice.entity.CustomerProfile;
import com.example.aicustomerservice.entity.MessageSenderType;
import com.example.aicustomerservice.entity.MessageVisibility;
import com.example.aicustomerservice.entity.SupportConversation;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 客服会话状态机。
 *
 * Controller 只负责接收请求；所有领取、转接、等待和解决规则都集中在这里。这样无论未来由网页、
 * 飞书机器人还是定时任务触发，会话都遵循同一套状态迁移和审计记录。
 */
public class ConversationService {

    /** 等待超过 15 分钟仍无人领取时计入运营看板的 SLA 风险。 */
    private static final long WAITING_SLA_MINUTES = 15;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final ConversationDao conversationDao;
    private final CustomerDao customerDao;
    private final Clock clock;

    public ConversationService(ConversationDao conversationDao, CustomerDao customerDao, Clock clock) {
        this.conversationDao = conversationDao;
        this.customerDao = customerDao;
        this.clock = clock;
    }

    public List<SupportConversation> search(
        ConversationStatus status,
        ConversationChannel channel,
        String assignee,
        String queueName,
        String keyword
    ) {
        return conversationDao.search(new ConversationQuery(status, channel, assignee, queueName, keyword));
    }

    public ConversationDetail get(String conversationId) {
        SupportConversation conversation = requireConversation(conversationId);
        CustomerProfile customer = customerDao.findByCustomerId(conversation.customerId())
            .orElseGet(() -> new CustomerProfile(
                conversation.customerId(),
                conversation.customerName(),
                conversation.customerTier(),
                ""
            ));
        return new ConversationDetail(
            conversation,
            customer,
            conversationDao.findMessages(conversationId)
        );
    }

    /**
     * 客服主动领取未分配会话。已经由同一个客服领取时按幂等成功处理；被其他人领取时明确报错。
     */
    public ConversationDetail claim(String conversationId, String operator) {
        String normalizedOperator = required(operator, "领取人不能为空");
        SupportConversation current = requireOpenConversation(conversationId);
        if (!current.assignee().isBlank()) {
            if (current.assignee().equals(normalizedOperator)) {
                return get(conversationId);
            }
            throw new BusinessException(
                "CONVERSATION_ALREADY_CLAIMED",
                "会话已由 " + current.assignee() + " 领取，请刷新收件箱后重试。"
            );
        }

        Instant now = clock.instant();
        SupportConversation updated = current.assignedTo(normalizedOperator, ConversationStatus.IN_PROGRESS, now);
        saveEvent(updated, normalizedOperator, "领取了会话。", now);
        return get(conversationId);
    }

    /**
     * 主管转接或直接分配会话。等待客户/内部结果的会话会保留原状态，避免转接动作误改业务阶段。
     */
    public ConversationDetail assign(String conversationId, String owner, String operator) {
        String normalizedOwner = required(owner, "新的处理人不能为空");
        String normalizedOperator = optional(operator, "system-supervisor");
        SupportConversation current = requireOpenConversation(conversationId);
        if (current.assignee().equals(normalizedOwner)) {
            return get(conversationId);
        }

        ConversationStatus nextStatus = current.status() == ConversationStatus.WAITING
            ? ConversationStatus.IN_PROGRESS
            : current.status();
        Instant now = clock.instant();
        String previousOwner = current.assignee().isBlank() ? "未分配" : current.assignee();
        SupportConversation updated = current.assignedTo(normalizedOwner, nextStatus, now);
        saveEvent(updated, normalizedOperator, "将会话从 " + previousOwner + " 转接给 " + normalizedOwner + "。", now);
        return get(conversationId);
    }

    /** 客服向客户发送可见回复；内部备注必须调用 addNote，避免误把内部信息暴露给客户。 */
    public ConversationDetail reply(String conversationId, String operator, String content) {
        String normalizedOperator = required(operator, "操作人不能为空");
        String normalizedContent = required(content, "回复内容不能为空");
        SupportConversation current = requireAssignedTo(conversationId, normalizedOperator);
        Instant now = clock.instant();
        SupportConversation updated = current.replied(normalizedContent, now);
        ConversationMessage message = message(
            conversationId,
            MessageSenderType.AGENT,
            normalizedOperator,
            MessageVisibility.CUSTOMER,
            normalizedContent,
            now
        );
        conversationDao.saveWithMessage(updated, message);
        return get(conversationId);
    }

    /** 添加团队内部备注，不改变客户可见的最近消息摘要。 */
    public ConversationDetail addNote(String conversationId, String operator, String content) {
        String normalizedOperator = required(operator, "操作人不能为空");
        String normalizedContent = required(content, "内部备注不能为空");
        SupportConversation current = requireAssignedTo(conversationId, normalizedOperator);
        ConversationMessage message = message(
            conversationId,
            MessageSenderType.AGENT,
            normalizedOperator,
            MessageVisibility.INTERNAL,
            normalizedContent,
            clock.instant()
        );
        // 内部备注不改变客户消息预览，但需要刷新会话更新时间以便审计最近一次操作。
        SupportConversation updated = new SupportConversation(
            current.conversationId(), current.customerId(), current.customerName(), current.customerTier(),
            current.channel(), current.status(), current.priority(), current.subject(), current.intent(),
            current.orderNo(), current.queueName(), current.assignee(), current.tags(), current.unreadCount(),
            current.lastMessagePreview(), current.lastMessageAt(), current.createdAt(), message.createdAt(),
            current.firstResponseAt(), current.resolvedAt(), current.resolution()
        );
        conversationDao.saveWithMessage(updated, message);
        return get(conversationId);
    }

    /** 将会话挂起到“等待客户”，并把原因写入内部时间线。 */
    public ConversationDetail waitForCustomer(String conversationId, String operator, String reason) {
        String normalizedOperator = required(operator, "操作人不能为空");
        String normalizedReason = required(reason, "等待原因不能为空");
        SupportConversation current = requireAssignedTo(conversationId, normalizedOperator);
        if (current.status() == ConversationStatus.WAITING_CUSTOMER) {
            return get(conversationId);
        }
        Instant now = clock.instant();
        SupportConversation updated = current.waiting(ConversationStatus.WAITING_CUSTOMER, now);
        saveEvent(updated, normalizedOperator, "会话已设为等待客户：" + normalizedReason, now);
        return get(conversationId);
    }

    /** 解决会话。解决后的会话禁止继续回复、备注或转接。 */
    public ConversationDetail resolve(String conversationId, String operator, String resolution) {
        String normalizedOperator = required(operator, "操作人不能为空");
        String normalizedResolution = required(resolution, "解决摘要不能为空");
        SupportConversation current = requireAssignedTo(conversationId, normalizedOperator);
        Instant now = clock.instant();
        SupportConversation updated = current.resolved(normalizedResolution, now);
        saveEvent(updated, normalizedOperator, "会话已解决：" + normalizedResolution, now);
        return get(conversationId);
    }

    public ConversationDashboard dashboard() {
        Instant now = clock.instant();
        List<SupportConversation> all = search(null, null, null, null, null);
        List<SupportConversation> waiting = all.stream()
            .filter(item -> item.status() == ConversationStatus.WAITING)
            .toList();
        long totalWaitingMinutes = waiting.stream()
            .mapToLong(item -> waitingMinutes(item, now))
            .sum();
        long averageWaitingMinutes = waiting.isEmpty() ? 0 : totalWaitingMinutes / waiting.size();
        long totalWaitingSeconds = waiting.stream()
            .mapToLong(item -> Math.max(0, Duration.between(item.lastMessageAt(), now).toSeconds()))
            .sum();
        long averageWaitSeconds = waiting.isEmpty() ? 0 : totalWaitingSeconds / waiting.size();

        List<SupportConversation> responded = all.stream()
            .filter(item -> item.firstResponseAt() != null)
            .toList();
        long averageFirstResponseSeconds = responded.isEmpty() ? 0 : responded.stream()
            .mapToLong(item -> Math.max(0, Duration.between(item.createdAt(), item.firstResponseAt()).toSeconds()))
            .sum() / responded.size();

        int resolvedCount = count(all, ConversationStatus.RESOLVED);
        int resolvedTodayCount = (int) all.stream()
            .filter(item -> item.resolvedAt() != null)
            .filter(item -> item.resolvedAt().atZone(BUSINESS_ZONE).toLocalDate()
                .equals(now.atZone(BUSINESS_ZONE).toLocalDate()))
            .count();
        int aiAssistedCount = (int) all.stream()
            .filter(item -> conversationDao.findMessages(item.conversationId()).stream()
                .anyMatch(message -> message.senderType() == MessageSenderType.AI))
            .count();
        double aiAssistRate = all.isEmpty() ? 0 : Math.round(aiAssistedCount * 1000.0 / all.size()) / 10.0;

        return new ConversationDashboard(
            all.size(),
            all.size() - resolvedCount,
            waiting.size(),
            count(all, ConversationStatus.IN_PROGRESS),
            count(all, ConversationStatus.WAITING_CUSTOMER),
            count(all, ConversationStatus.WAITING_INTERNAL),
            resolvedCount,
            resolvedTodayCount,
            (int) all.stream().filter(item -> item.status() != ConversationStatus.RESOLVED && item.assignee().isBlank()).count(),
            (int) all.stream().filter(item -> item.status() != ConversationStatus.RESOLVED && item.priority() == ConversationPriority.URGENT).count(),
            (int) waiting.stream().filter(item -> waitingMinutes(item, now) >= WAITING_SLA_MINUTES).count(),
            averageWaitingMinutes,
            averageWaitSeconds,
            averageFirstResponseSeconds,
            aiAssistRate,
            null,
            countByChannel(all),
            countByStatus(all),
            now
        );
    }

    private void saveEvent(
        SupportConversation conversation,
        String operator,
        String content,
        Instant now
    ) {
        conversationDao.saveWithMessage(
            conversation,
            message(
                conversation.conversationId(),
                MessageSenderType.SYSTEM,
                operator,
                MessageVisibility.INTERNAL,
                content,
                now
            )
        );
    }

    private ConversationMessage message(
        String conversationId,
        MessageSenderType senderType,
        String sender,
        MessageVisibility visibility,
        String content,
        Instant now
    ) {
        String messageId = "MSG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        return new ConversationMessage(messageId, conversationId, senderType, sender, visibility, content, now);
    }

    private SupportConversation requireConversation(String conversationId) {
        return conversationDao.findByConversationId(conversationId)
            .orElseThrow(() -> new BusinessException("CONVERSATION_NOT_FOUND", "会话不存在：" + conversationId));
    }

    private SupportConversation requireOpenConversation(String conversationId) {
        SupportConversation conversation = requireConversation(conversationId);
        if (conversation.status() == ConversationStatus.RESOLVED) {
            throw new BusinessException("CONVERSATION_RESOLVED", "已解决会话不能继续操作：" + conversationId);
        }
        return conversation;
    }

    private SupportConversation requireAssignedTo(String conversationId, String operator) {
        SupportConversation conversation = requireOpenConversation(conversationId);
        if (conversation.assignee().isBlank()) {
            throw new BusinessException("CONVERSATION_NOT_ASSIGNED", "请先领取会话再进行处理：" + conversationId);
        }
        if (!conversation.assignee().equals(operator)) {
            throw new BusinessException(
                "CONVERSATION_OPERATOR_MISMATCH",
                "会话当前由 " + conversation.assignee() + " 处理，" + operator + " 无权直接操作。"
            );
        }
        return conversation;
    }

    private int count(List<SupportConversation> conversations, ConversationStatus status) {
        return (int) conversations.stream().filter(item -> item.status() == status).count();
    }

    private Map<String, Integer> countByChannel(List<SupportConversation> conversations) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ConversationChannel channel : ConversationChannel.values()) {
            counts.put(channel.name(), (int) conversations.stream().filter(item -> item.channel() == channel).count());
        }
        return counts;
    }

    private Map<String, Integer> countByStatus(List<SupportConversation> conversations) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ConversationStatus status : ConversationStatus.values()) {
            counts.put(status.name(), count(conversations, status));
        }
        return counts;
    }

    private long waitingMinutes(SupportConversation conversation, Instant now) {
        return Math.max(0, Duration.between(conversation.lastMessageAt(), now).toMinutes());
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("CONVERSATION_REQUEST_INVALID", message);
        }
        return value.trim();
    }

    private String optional(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
