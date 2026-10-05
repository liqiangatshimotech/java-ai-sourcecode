package com.example.aicustomerservice.service;

import com.example.aicustomerservice.common.BusinessException;
import com.example.aicustomerservice.dao.InMemoryConversationDao;
import com.example.aicustomerservice.dao.InMemoryCustomerDao;
import com.example.aicustomerservice.entity.ConversationChannel;
import com.example.aicustomerservice.entity.ConversationDashboard;
import com.example.aicustomerservice.entity.ConversationDetail;
import com.example.aicustomerservice.entity.ConversationMessage;
import com.example.aicustomerservice.entity.ConversationPriority;
import com.example.aicustomerservice.entity.ConversationStatus;
import com.example.aicustomerservice.entity.CustomerTier;
import com.example.aicustomerservice.entity.MessageSenderType;
import com.example.aicustomerservice.entity.MessageVisibility;
import com.example.aicustomerservice.entity.SupportConversation;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T08:00:00Z");
    private static final String CONVERSATION_ID = "CONV-TEST-001";
    private static final String CUSTOMER_MESSAGE_ID = "MSG-CUSTOMER-001";

    private Clock clock;
    private InMemoryConversationDao conversationDao;
    private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        // 固定时间后，等待时长、首响时长和解决日期的断言都不会因运行速度而抖动。
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        conversationDao = new InMemoryConversationDao(clock, false);
        conversationService = new ConversationService(
            conversationDao,
            new InMemoryCustomerDao(),
            clock
        );

        // 测试数据显式写入，不依赖演示种子，确保用例只验证当前业务规则。
        SupportConversation waiting = conversation(
            CONVERSATION_ID,
            ConversationStatus.WAITING,
            ConversationChannel.WECHAT,
            ConversationPriority.URGENT,
            "",
            NOW.minusSeconds(30 * 60),
            NOW.minusSeconds(45 * 60),
            null,
            null,
            ""
        );
        conversationDao.save(waiting);
        conversationDao.appendMessage(new ConversationMessage(
            CUSTOMER_MESSAGE_ID,
            CONVERSATION_ID,
            MessageSenderType.CUSTOMER,
            "北京朝阳门店",
            MessageVisibility.CUSTOMER,
            "这个订单还能取消吗？",
            waiting.lastMessageAt()
        ));
    }

    @Test
    void shouldCompleteClaimReplyWaitCustomerAndResolveWorkflow() {
        ConversationDetail claimed = conversationService.claim(CONVERSATION_ID, "  王芳  ");

        assertThat(claimed.conversation().status()).isEqualTo(ConversationStatus.IN_PROGRESS);
        assertThat(claimed.conversation().assignee()).isEqualTo("王芳");
        assertThat(claimed.conversation().unreadCount()).isZero();
        assertThat(claimed.customer().phoneMasked()).isEqualTo("138****0001");
        assertThat(claimed.messages())
            .anySatisfy(message -> {
                assertThat(message.senderType()).isEqualTo(MessageSenderType.SYSTEM);
                assertThat(message.visibility()).isEqualTo(MessageVisibility.INTERNAL);
                assertThat(message.content()).isEqualTo("领取了会话。");
            });

        // 同一个人重复领取是幂等操作，不应重复写入系统事件。
        conversationService.claim(CONVERSATION_ID, "王芳");
        assertThat(conversationDao.findMessages(CONVERSATION_ID)).hasSize(2);

        ConversationDetail replied = conversationService.reply(
            CONVERSATION_ID,
            "王芳",
            "  已为您查询，请补充取消原因。  "
        );
        assertThat(replied.conversation().status()).isEqualTo(ConversationStatus.IN_PROGRESS);
        assertThat(replied.conversation().firstResponseAt()).isEqualTo(NOW);
        assertThat(replied.conversation().lastMessageAt()).isEqualTo(NOW);
        assertThat(replied.conversation().lastMessagePreview()).isEqualTo("已为您查询，请补充取消原因。");
        assertThat(replied.messages())
            .anySatisfy(message -> {
                assertThat(message.senderType()).isEqualTo(MessageSenderType.AGENT);
                assertThat(message.sender()).isEqualTo("王芳");
                assertThat(message.visibility()).isEqualTo(MessageVisibility.CUSTOMER);
                assertThat(message.content()).isEqualTo("已为您查询，请补充取消原因。");
            });

        ConversationDetail waitingCustomer = conversationService.waitForCustomer(
            CONVERSATION_ID,
            "王芳",
            "等待客户提供取消原因"
        );
        assertThat(waitingCustomer.conversation().status()).isEqualTo(ConversationStatus.WAITING_CUSTOMER);
        assertThat(waitingCustomer.conversation().lastMessagePreview())
            .isEqualTo("已为您查询，请补充取消原因。");
        assertThat(waitingCustomer.messages())
            .anyMatch(message -> message.content().equals("会话已设为等待客户：等待客户提供取消原因"));

        ConversationDetail resolved = conversationService.resolve(
            CONVERSATION_ID,
            "王芳",
            "客户确认保留订单"
        );
        assertThat(resolved.conversation().status()).isEqualTo(ConversationStatus.RESOLVED);
        assertThat(resolved.conversation().resolvedAt()).isEqualTo(NOW);
        assertThat(resolved.conversation().resolution()).isEqualTo("客户确认保留订单");
        assertThat(resolved.messages()).hasSize(5);
        assertThat(resolved.messages()).filteredOn(
            message -> message.senderType() == MessageSenderType.SYSTEM
        ).hasSize(3);
        assertThat(resolved.messages())
            .anyMatch(message -> message.content().equals("会话已解决：客户确认保留订单"));
    }

    @Test
    void shouldPreserveWaitingCustomerStatusWhenSupervisorTransfersConversation() {
        conversationService.claim(CONVERSATION_ID, "王芳");
        conversationService.waitForCustomer(CONVERSATION_ID, "王芳", "等待收货地址");

        ConversationDetail transferred = conversationService.assign(CONVERSATION_ID, "李杰", "班组主管");

        assertThat(transferred.conversation().status()).isEqualTo(ConversationStatus.WAITING_CUSTOMER);
        assertThat(transferred.conversation().assignee()).isEqualTo("李杰");
        assertThat(transferred.messages())
            .anySatisfy(message -> {
                assertThat(message.senderType()).isEqualTo(MessageSenderType.SYSTEM);
                assertThat(message.sender()).isEqualTo("班组主管");
                assertThat(message.visibility()).isEqualTo(MessageVisibility.INTERNAL);
                assertThat(message.content()).isEqualTo("将会话从 王芳 转接给 李杰。");
            });

        assertBusinessError(
            "CONVERSATION_OPERATOR_MISMATCH",
            () -> conversationService.addNote(CONVERSATION_ID, "王芳", "原处理人尝试写入")
        );
        assertThat(conversationService.addNote(CONVERSATION_ID, "李杰", "已完成交接").messages())
            .anyMatch(message -> message.content().equals("已完成交接"));
    }

    @Test
    void shouldRejectClaimWhenConversationBelongsToAnotherAgent() {
        conversationService.claim(CONVERSATION_ID, "王芳");

        assertBusinessError(
            "CONVERSATION_ALREADY_CLAIMED",
            () -> conversationService.claim(CONVERSATION_ID, "李杰")
        );
        assertThat(conversationDao.findByConversationId(CONVERSATION_ID).orElseThrow().assignee())
            .isEqualTo("王芳");
        assertThat(conversationDao.findMessages(CONVERSATION_ID)).hasSize(2);
    }

    @Test
    void shouldRejectEveryWriteAttemptAfterConversationIsResolved() {
        conversationService.claim(CONVERSATION_ID, "王芳");
        conversationService.resolve(CONVERSATION_ID, "王芳", "问题已处理");
        int messageCountBeforeRejectedWrites = conversationDao.findMessages(CONVERSATION_ID).size();

        assertBusinessError(
            "CONVERSATION_RESOLVED",
            () -> conversationService.reply(CONVERSATION_ID, "王芳", "不应发送的回复")
        );
        assertBusinessError(
            "CONVERSATION_RESOLVED",
            () -> conversationService.addNote(CONVERSATION_ID, "王芳", "不应保存的备注")
        );
        assertBusinessError(
            "CONVERSATION_RESOLVED",
            () -> conversationService.assign(CONVERSATION_ID, "李杰", "班组主管")
        );
        assertBusinessError(
            "CONVERSATION_RESOLVED",
            () -> conversationService.waitForCustomer(CONVERSATION_ID, "王芳", "不应挂起")
        );

        assertThat(conversationDao.findMessages(CONVERSATION_ID))
            .hasSize(messageCountBeforeRejectedWrites);
    }

    @Test
    void shouldCalculateDashboardFromPersistedConversationsAndMessages() {
        SupportConversation inProgress = conversation(
            "CONV-TEST-002",
            ConversationStatus.IN_PROGRESS,
            ConversationChannel.APP,
            ConversationPriority.NORMAL,
            "张敏",
            NOW.minusSeconds(2 * 60),
            NOW.minusSeconds(10 * 60),
            NOW.minusSeconds(5 * 60),
            null,
            ""
        );
        conversationDao.save(inProgress);
        conversationDao.appendMessage(new ConversationMessage(
            "MSG-AI-002",
            inProgress.conversationId(),
            MessageSenderType.AI,
            "ai-copilot",
            MessageVisibility.INTERNAL,
            "建议核对物流轨迹。",
            NOW.minusSeconds(6 * 60)
        ));

        SupportConversation resolved = conversation(
            "CONV-TEST-003",
            ConversationStatus.RESOLVED,
            ConversationChannel.EMAIL,
            ConversationPriority.LOW,
            "李杰",
            NOW.minusSeconds(2 * 3600),
            NOW.minusSeconds(3 * 3600),
            null,
            NOW.minusSeconds(3600),
            "已完成处理"
        );
        conversationDao.save(resolved);

        ConversationDashboard dashboard = conversationService.dashboard();

        assertThat(dashboard.totalCount()).isEqualTo(3);
        assertThat(dashboard.openCount()).isEqualTo(2);
        assertThat(dashboard.waitingCount()).isEqualTo(1);
        assertThat(dashboard.inProgressCount()).isEqualTo(1);
        assertThat(dashboard.waitingCustomerCount()).isZero();
        assertThat(dashboard.waitingInternalCount()).isZero();
        assertThat(dashboard.resolvedCount()).isEqualTo(1);
        assertThat(dashboard.resolvedTodayCount()).isEqualTo(1);
        assertThat(dashboard.unassignedCount()).isEqualTo(1);
        assertThat(dashboard.urgentCount()).isEqualTo(1);
        assertThat(dashboard.slaRiskCount()).isEqualTo(1);
        assertThat(dashboard.averageWaitingMinutes()).isEqualTo(30);
        assertThat(dashboard.averageWaitSeconds()).isEqualTo(30 * 60);
        assertThat(dashboard.averageFirstResponseSeconds()).isEqualTo(5 * 60);
        assertThat(dashboard.aiAssistRate()).isEqualTo(33.3);
        assertThat(dashboard.channelCounts())
            .containsEntry("WECHAT", 1)
            .containsEntry("APP", 1)
            .containsEntry("EMAIL", 1)
            .containsEntry("WEB", 0)
            .containsEntry("HOTLINE", 0);
        assertThat(dashboard.statusCounts())
            .containsEntry("WAITING", 1)
            .containsEntry("IN_PROGRESS", 1)
            .containsEntry("RESOLVED", 1);
        assertThat(dashboard.updatedAt()).isEqualTo(NOW);
    }

    private SupportConversation conversation(
        String conversationId,
        ConversationStatus status,
        ConversationChannel channel,
        ConversationPriority priority,
        String assignee,
        Instant lastMessageAt,
        Instant createdAt,
        Instant firstResponseAt,
        Instant resolvedAt,
        String resolution
    ) {
        return new SupportConversation(
            conversationId,
            "C10001",
            "北京朝阳门店",
            CustomerTier.ENTERPRISE,
            channel,
            status,
            priority,
            "高价值订单取消",
            "REFUND",
            "SO202606210004",
            "售后二线",
            assignee,
            List.of("企业客户", "退款"),
            status == ConversationStatus.WAITING ? 2 : 0,
            "这个订单还能取消吗？",
            lastMessageAt,
            createdAt,
            lastMessageAt,
            firstResponseAt,
            resolvedAt,
            resolution
        );
    }

    private void assertBusinessError(String expectedCode, ThrowingCallable operation) {
        assertThatThrownBy(operation)
            .isInstanceOfSatisfying(BusinessException.class, exception -> {
                assertThat(exception.code()).isEqualTo(expectedCode);
                assertThat(exception.getMessage()).isNotBlank();
            });
    }
}
