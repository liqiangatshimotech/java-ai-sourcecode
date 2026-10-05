package com.example.aicustomerservice.service;

import com.example.aicustomerservice.dao.InMemoryCustomerDao;
import com.example.aicustomerservice.dao.InMemoryKnowledgeDao;
import com.example.aicustomerservice.dao.InMemoryOrderDao;
import com.example.aicustomerservice.dao.InMemoryTicketDao;
import com.example.aicustomerservice.entity.EnterpriseProblemType;
import com.example.aicustomerservice.entity.SupportDecision;
import com.example.aicustomerservice.test.FakeChatModelClient;
import java.time.Clock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerSupportServiceTest {

    @Test
    void refundDuringPickingShouldCreateTicketAndReuseDuplicate() {
        CustomerSupportService supportService = newCustomerSupportService();

        SupportDecision first = supportService.handle(
            "S-1",
            "C10001",
            "这个订单现在不想要了，能直接退款吗？",
            "SO202606210001"
        );
        SupportDecision second = supportService.handle(
            "S-1",
            "C10001",
            "这个订单现在不想要了，能直接退款吗？",
            "SO202606210001"
        );

        assertThat(first.handoffRequired()).isTrue();
        assertThat(first.ticket()).isNotNull();
        assertThat(first.ticket().priority().name()).isEqualTo("URGENT");
        assertThat(second.ticket().ticketId()).isEqualTo(first.ticket().ticketId());
        assertThat(second.ticket().duplicateCount()).isEqualTo(1);
    }

    @Test
    void logisticsQueryWithOrderNoShouldUseToolWithoutTicket() {
        CustomerSupportService supportService = newCustomerSupportService();

        SupportDecision decision = supportService.handle(
            "S-2",
            "C10002",
            "帮我查一下物流到哪里了",
            "SO202606210002"
        );

        assertThat(decision.handoffRequired()).isFalse();
        assertThat(decision.ticket()).isNull();
        assertThat(decision.toolTraces()).anyMatch(trace -> trace.toolName().equals("logistics-api"));
        assertThat(decision.answer()).contains("杭州转运中心");
    }

    @Test
    void dailyNecessitiesRefundShouldUseProductRuleWithoutTicket() {
        CustomerSupportService supportService = newCustomerSupportService();

        SupportDecision decision = supportService.handle(
            "S-daily",
            "C10003",
            "这个订单可以退货吗？",
            "SO202606210003"
        );

        assertThat(decision.handoffRequired()).isFalse();
        assertThat(decision.ticket()).isNull();
        assertThat(decision.toolTraces())
            .anyMatch(trace -> trace.toolName().equals("order-api") && trace.summary().contains("日用品组合"));
        assertThat(decision.toolTraces())
            .anyMatch(trace -> trace.toolName().equals("refund-policy-engine") && trace.summary().contains("日用品"));
        assertThat(decision.answer()).contains("日用品", "不支持退货");
    }

    @Test
    void unknownQuestionShouldTriggerHallucinationGuardrail() {
        CustomerSupportService supportService = newCustomerSupportService();

        SupportDecision decision = supportService.handle(
            "S-3",
            "C10003",
            "页面跳转失败但我也说不清楚是哪里坏了",
            null
        );

        assertThat(decision.handoffRequired()).isTrue();
        assertThat(decision.guardrails())
            .anyMatch(guardrail -> guardrail.problem() == EnterpriseProblemType.HALLUCINATION_RISK);
    }

    @Test
    void faqAnswerShouldUseHotQuestionCacheOnSecondRequest() {
        CustomerSupportService supportService = newCustomerSupportService();

        SupportDecision first = supportService.handle(
            "S-4",
            "C10003",
            "发票抬头写错了怎么重开",
            null
        );
        SupportDecision second = supportService.handle(
            "S-5",
            "C10003",
            "发票抬头写错了怎么重开",
            null
        );

        assertThat(first.cachedAnswer()).isFalse();
        assertThat(second.cachedAnswer()).isTrue();
        assertThat(second.modelTier().name()).isEqualTo("FAST");
    }

    @Test
    void zeroCapacityShouldBeRejectedAsInvalidConfiguration() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> newCustomerSupportService(0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void logisticsMustNotExposeAnotherCustomersTrackingNumber() {
        SupportDecision decision = newCustomerSupportService().handle(
            "S-cross-customer", "C10003", "帮我查一下物流", "SO202606210002");
        assertThat(decision.handoffRequired()).isTrue();
        assertThat(decision.toolTraces()).allMatch(trace -> !trace.summary().contains("SF"));
        assertThat(decision.toolTraces()).anyMatch(trace -> trace.summary().contains("没有查到客户名下订单"));
    }

    private CustomerSupportService newCustomerSupportService() {
        return newCustomerSupportService(16);
    }

    private CustomerSupportService newCustomerSupportService(int maxImmediateRequests) {
        Clock clock = Clock.systemUTC();
        var knowledgeDao = new InMemoryKnowledgeDao(clock);
        var orderDao = new InMemoryOrderDao(clock);
        var ticketDao = new InMemoryTicketDao();
        var model = new FakeChatModelClient();
        return new CustomerSupportService(
            new InMemoryCustomerDao(),
            new IntentRecognitionService(),
            new RetrievalService(knowledgeDao, new SemanticChunker()),
            new InMemoryCustomerToolService(orderDao),
            new QueuedModelExecutor(model, maxImmediateRequests),
            new TicketService(ticketDao, new TicketIdGenerator(clock), clock),
            new ConversationMemoryService(),
            new ProductionGuardrailService(),
            new InMemoryHotQuestionCache()
        );
    }
}
