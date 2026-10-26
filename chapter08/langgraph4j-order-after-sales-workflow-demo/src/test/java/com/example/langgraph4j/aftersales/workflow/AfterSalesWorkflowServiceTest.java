package com.example.langgraph4j.aftersales.workflow;

import com.example.langgraph4j.aftersales.ai.AfterSalesAssistant;
import com.example.langgraph4j.aftersales.ai.RuleBasedAfterSalesAssistant;
import com.example.langgraph4j.aftersales.config.WorkflowProperties;
import com.example.langgraph4j.aftersales.domain.*;
import com.example.langgraph4j.aftersales.service.*;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/** 真实运行 LangGraph4j 图，但模型和 checkpoint 都是确定性的离线实现。 */
class AfterSalesWorkflowServiceTest {

    @Test
    void missingInformationCanResumeThenPauseForApprovalAndResumeAgain() throws Exception {
        TestFixture fixture = fixture(3, new RuleBasedAfterSalesAssistant());

        WorkflowExecutionResult first = fixture.service.start("flow-1001", "我想申请退款");
        assertEquals(WorkflowStatus.WAITING_FOR_INFORMATION, first.status());
        assertEquals(AfterSalesWorkflow.WAIT_FOR_INFORMATION, first.nextNode());

        List<WorkflowEvent> events = new ArrayList<>();
        WorkflowExecutionResult second = fixture.service.provideInformation(
                "flow-1001", "订单号 ORD-1001", events::add);
        assertEquals(WorkflowStatus.WAITING_FOR_APPROVAL, second.status());
        assertEquals(AfterSalesWorkflow.HUMAN_APPROVAL, second.nextNode());
        assertTrue(second.trace().stream().anyMatch(value -> value.startsWith("policy_check:")));
        assertTrue(second.trace().stream().anyMatch(value -> value.startsWith("risk_check:")));
        assertTrue(events.stream().anyMatch(event -> event.node().startsWith("__PARALLEL__")));

        WorkflowExecutionResult completed = fixture.service.approve(
                "flow-1001", ApprovalDecision.APPROVED, "reviewer-a");
        assertEquals(WorkflowStatus.COMPLETED, completed.status());
        assertFalse(completed.refundTransactionId().isBlank());
        assertEquals(1, fixture.refundService.executedCount());
        assertThrows(IllegalStateException.class, () -> fixture.service.approve(
                "flow-1001", ApprovalDecision.APPROVED, "reviewer-a"));
    }

    @Test
    void lowRiskRefundExecutesWithoutHumanApproval() throws Exception {
        TestFixture fixture = fixture(3, new RuleBasedAfterSalesAssistant());
        WorkflowExecutionResult result = fixture.service.start(
                "flow-auto", "订单 ORD-1002 申请退款");

        assertEquals(WorkflowStatus.COMPLETED, result.status());
        assertTrue(result.nextNode().isBlank());
        assertFalse(result.refundTransactionId().isBlank());
        assertEquals(1, fixture.refundService.executedCount());
    }

    @Test
    void highRiskRefundIsDeniedWithoutExecutingMoneyOperation() throws Exception {
        TestFixture fixture = fixture(3, new RuleBasedAfterSalesAssistant());
        WorkflowExecutionResult result = fixture.service.start(
                "flow-risk", "订单 ORD-9009 申请退款");

        assertEquals(WorkflowStatus.COMPLETED, result.status());
        assertEquals("DENIED_RISK", result.state().get(AfterSalesState.REFUND_DECISION));
        assertTrue(result.refundTransactionId().isBlank());
        assertEquals(0, fixture.refundService.executedCount());
    }

    @Test
    void reviewerCanRejectRefund() throws Exception {
        TestFixture fixture = fixture(3, new RuleBasedAfterSalesAssistant());
        WorkflowExecutionResult paused = fixture.service.start(
                "flow-reject", "订单 ORD-1001 申请退款");
        assertEquals(WorkflowStatus.WAITING_FOR_APPROVAL, paused.status());

        WorkflowExecutionResult result = fixture.service.approve(
                "flow-reject", ApprovalDecision.REJECTED, "reviewer-b");
        assertEquals(WorkflowStatus.COMPLETED, result.status());
        assertEquals("REJECTED_BY_REVIEWER", result.state().get(AfterSalesState.REFUND_DECISION));
        assertEquals(0, fixture.refundService.executedCount());
    }

    @Test
    void clarificationBusinessLimitRoutesToFailureNode() throws Exception {
        TestFixture fixture = fixture(1, new RuleBasedAfterSalesAssistant());
        WorkflowExecutionResult paused = fixture.service.start("flow-limit", "我要退款");
        assertEquals(WorkflowStatus.WAITING_FOR_INFORMATION, paused.status());

        WorkflowExecutionResult failed = fixture.service.provideInformation("flow-limit", "还没找到订单号");
        assertEquals(WorkflowStatus.FAILED, failed.status());
        assertTrue(failed.error().contains("上限"));
        assertTrue(failed.trace().stream().anyMatch(value -> value.startsWith("failure:")));
    }

    @Test
    void modelFailureBecomesAuditableFailedState() throws Exception {
        AfterSalesAssistant brokenAssistant = new AfterSalesAssistant() {
            @Override
            public IntentAnalysis analyzeRequest(String userMessage, String knownOrderId) {
                throw new IllegalStateException("模型服务暂时不可用");
            }

            @Override
            public String answerConsultation(String userMessage, OrderSnapshot order) { return ""; }

            @Override
            public String explainRefundResult(String decision, OrderSnapshot order, String detail) { return ""; }
        };
        TestFixture fixture = fixture(3, brokenAssistant);

        WorkflowExecutionResult result = fixture.service.start("flow-error", "订单 ORD-1001 申请退款");
        assertEquals(WorkflowStatus.FAILED, result.status());
        assertEquals(AfterSalesWorkflow.UNDERSTAND_REQUEST,
                result.state().get(AfterSalesState.FAILED_NODE));
        assertTrue(result.error().contains("暂时不可用"));
    }

    @Test
    void responseModelFailureAfterRefundStillEndsAtFailureNode() throws Exception {
        RuleBasedAfterSalesAssistant assistant = new RuleBasedAfterSalesAssistant() {
            @Override
            public String explainRefundResult(String decision, OrderSnapshot order, String detail) {
                throw new IllegalStateException("回复模型超时");
            }
        };
        TestFixture fixture = fixture(3, assistant);

        WorkflowExecutionResult result = fixture.service.start(
                "flow-response-error", "订单 ORD-1002 申请退款");
        assertEquals(WorkflowStatus.FAILED, result.status());
        assertEquals(1, fixture.refundService.executedCount());
        assertTrue(result.error().contains("超时"));
        assertTrue(result.trace().stream().anyMatch(value -> value.startsWith("failure:")));
    }

    @Test
    void checkpointHistoryAndMermaidAreAvailable() throws Exception {
        TestFixture fixture = fixture(3, new RuleBasedAfterSalesAssistant());
        fixture.service.start("flow-history", "订单 ORD-1002 是什么商品");

        assertFalse(fixture.service.history("flow-history").isEmpty());
        assertTrue(fixture.service.mermaid().contains(AfterSalesWorkflow.HUMAN_APPROVAL));
    }

    @Test
    void policyAndRiskChecksReallyStartConcurrently() throws Exception {
        CountDownLatch bothStarted = new CountDownLatch(2);
        AtomicBoolean policySawRisk = new AtomicBoolean();
        AtomicBoolean riskSawPolicy = new AtomicBoolean();
        RefundPolicyService policy = new RefundPolicyService() {
            @Override
            public PolicyCheckResult check(OrderSnapshot order) {
                bothStarted.countDown();
                policySawRisk.set(awaitBoth(bothStarted));
                return super.check(order);
            }
        };
        RefundRiskService risk = new RefundRiskService() {
            @Override
            public RiskCheckResult check(OrderSnapshot order) {
                bothStarted.countDown();
                riskSawPolicy.set(awaitBoth(bothStarted));
                return super.check(order);
            }
        };
        ExecutorService parallelExecutor = Executors.newFixedThreadPool(2);
        try {
            WorkflowProperties properties = new WorkflowProperties();
            IdempotentRefundExecutionService refund = new IdempotentRefundExecutionService();
            AfterSalesWorkflow workflow = new AfterSalesWorkflow(
                    new RuleBasedAfterSalesAssistant(), new InMemoryOrderQueryService(), policy, risk,
                    refund, properties, new MemorySaver());
            AfterSalesWorkflowService service = new AfterSalesWorkflowService(workflow, parallelExecutor);

            WorkflowExecutionResult result = service.start("flow-parallel", "订单 ORD-1002 申请退款");
            assertEquals(WorkflowStatus.COMPLETED, result.status());
            assertTrue(policySawRisk.get(), "policy 分支应在等待时看到 risk 已启动");
            assertTrue(riskSawPolicy.get(), "risk 分支应在等待时看到 policy 已启动");
            assertEquals(result.trace().size(), new HashSet<>(result.trace()).size(),
                    "ParallelNode 合并完整 state 时不应重复追加 fork 前的 trace：" + result.trace());
            assertEquals(1, result.trace().stream()
                    .filter("prepare_refund_checks:fork-policy-and-risk"::equals)
                    .count());
        }
        finally {
            parallelExecutor.shutdownNow();
        }
    }

    private static boolean awaitBoth(CountDownLatch latch) {
        try {
            return latch.await(2, TimeUnit.SECONDS);
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static TestFixture fixture(int maxClarifications, AfterSalesAssistant assistant) throws Exception {
        WorkflowProperties properties = new WorkflowProperties();
        properties.setMaxClarificationRounds(maxClarifications);
        properties.setRecursionLimit(40);
        IdempotentRefundExecutionService refundService = new IdempotentRefundExecutionService();
        AfterSalesWorkflow workflow = new AfterSalesWorkflow(
                assistant,
                new InMemoryOrderQueryService(),
                new RefundPolicyService(),
                new RefundRiskService(),
                refundService,
                properties,
                new MemorySaver());
        return new TestFixture(new AfterSalesWorkflowService(workflow, ForkJoinPool.commonPool()), refundService);
    }

    private record TestFixture(
            AfterSalesWorkflowService service,
            IdempotentRefundExecutionService refundService) {
    }

    @Test void duplicateThreadCannotOverwritePendingOrder() throws Exception {
        TestFixture fixture=fixture(3,new RuleBasedAfterSalesAssistant());
        fixture.service.start("duplicate","订单 ORD-1001 退款");
        assertThrows(IllegalStateException.class,() -> fixture.service.start("duplicate","订单 ORD-1002 退款"));
        assertEquals("ORD-1001",fixture.service.state("duplicate").orderId());
        assertEquals(WorkflowStatus.WAITING_FOR_APPROVAL,fixture.service.state("duplicate").status());
    }
    @Test void emptyOrderAlwaysCountsAsMissingInformation() {
        var result=new IntentAnalysis(AfterSalesIntent.REFUND,"",List.of());
        assertTrue(result.missingFields().contains("orderId"));
    }
}
