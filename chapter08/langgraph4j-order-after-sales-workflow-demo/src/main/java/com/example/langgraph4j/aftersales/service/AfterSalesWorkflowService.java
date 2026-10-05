package com.example.langgraph4j.aftersales.service;

import com.example.langgraph4j.aftersales.domain.ApprovalDecision;
import com.example.langgraph4j.aftersales.domain.WorkflowStatus;
import com.example.langgraph4j.aftersales.workflow.*;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.state.StateSnapshot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static org.bsc.langgraph4j.StateGraph.END;

/**
 * 工作流应用服务：负责 threadId、checkpoint 恢复、状态校验和节点事件转换。
 */
@Service
public class AfterSalesWorkflowService {

    private final AfterSalesWorkflow workflow;
    private final Executor refundCheckExecutor;
    private final Map<String, ReentrantLock> threadLocks = new ConcurrentHashMap<>();

    public AfterSalesWorkflowService(
            AfterSalesWorkflow workflow,
            @Qualifier("refundCheckExecutor") Executor refundCheckExecutor) {
        this.workflow = workflow;
        this.refundCheckExecutor = refundCheckExecutor;
    }

    public WorkflowExecutionResult start(String requestedThreadId, String message) {
        return start(requestedThreadId, message, ignored -> { });
    }

    public WorkflowExecutionResult start(
            String requestedThreadId, String message, Consumer<WorkflowEvent> eventConsumer) {
        String threadId = normalizeThreadId(requestedThreadId);
        return locked(threadId, () -> {
            RunnableConfig config = runnableConfig(threadId);
            Map<String, Object> input = Map.of(
                    AfterSalesState.THREAD_ID, threadId,
                    AfterSalesState.USER_MESSAGE, requireText(message, "message"),
                    AfterSalesState.STATUS, WorkflowStatus.RUNNING.name(),
                    AfterSalesState.CLARIFICATION_ROUNDS, 0);
            return run(threadId, input, config, eventConsumer);
        });
    }

    public WorkflowExecutionResult provideInformation(String threadId, String message) {
        return provideInformation(threadId, message, ignored -> { });
    }

    public WorkflowExecutionResult provideInformation(
            String threadId, String message, Consumer<WorkflowEvent> eventConsumer) {
        String normalized = requireThreadId(threadId);
        return locked(normalized, () -> {
            StateSnapshot<AfterSalesState> snapshot = currentSnapshot(normalized);
            requireNext(snapshot, AfterSalesWorkflow.WAIT_FOR_INFORMATION);
            RunnableConfig updated = workflow.graph().updateState(snapshot.config(), Map.of(
                    AfterSalesState.USER_MESSAGE, requireText(message, "message")));
            return run(normalized, null, updated, eventConsumer);
        });
    }

    public WorkflowExecutionResult approve(
            String threadId, ApprovalDecision decision, String approver) {
        return approve(threadId, decision, approver, ignored -> { });
    }

    public WorkflowExecutionResult approve(
            String threadId,
            ApprovalDecision decision,
            String approver,
            Consumer<WorkflowEvent> eventConsumer) {
        String normalized = requireThreadId(threadId);
        if (decision == null || decision == ApprovalDecision.PENDING) {
            throw new IllegalArgumentException("decision 必须是 APPROVED 或 REJECTED");
        }
        return locked(normalized, () -> {
            StateSnapshot<AfterSalesState> snapshot = currentSnapshot(normalized);
            requireNext(snapshot, AfterSalesWorkflow.HUMAN_APPROVAL);
            String refundDecision = decision == ApprovalDecision.APPROVED
                    ? "APPROVED_BY_REVIEWER" : "REJECTED_BY_REVIEWER";
            String detail = decision == ApprovalDecision.APPROVED
                    ? "审批人同意退款" : "审批人拒绝退款";
            RunnableConfig updated = workflow.graph().updateState(snapshot.config(), Map.of(
                    AfterSalesState.APPROVAL_DECISION, decision.name(),
                    AfterSalesState.APPROVER, requireText(approver, "approver"),
                    AfterSalesState.REFUND_DECISION, refundDecision,
                    AfterSalesState.DECISION_DETAIL, detail));
            return run(normalized, null, updated, eventConsumer);
        });
    }

    public WorkflowExecutionResult state(String threadId) {
        StateSnapshot<AfterSalesState> snapshot = currentSnapshot(requireThreadId(threadId));
        return toResult(threadId, snapshot.state(), snapshot.next());
    }

    public List<Map<String, Object>> history(String threadId) {
        RunnableConfig config = runnableConfig(requireThreadId(threadId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (StateSnapshot<AfterSalesState> snapshot : workflow.graph().getStateHistory(config)) {
            result.add(Map.of(
                    "node", snapshot.node(),
                    "nextNode", snapshot.next() == null ? "" : snapshot.next(),
                    "state", snapshot.state().publicView()));
        }
        return List.copyOf(result);
    }

    public String mermaid() {
        return workflow.mermaid();
    }

    private WorkflowExecutionResult run(
            String threadId,
            Map<String, Object> input,
            RunnableConfig config,
            Consumer<WorkflowEvent> eventConsumer) {
        AtomicLong sequence = new AtomicLong();
        try {
            for (NodeOutput<AfterSalesState> output : workflow.graph().stream(input, config)) {
                AfterSalesState state = output.state();
                eventConsumer.accept(new WorkflowEvent(
                        sequence.incrementAndGet(), threadId, output.node(), state.status(),
                        eventMessage(output), state.publicView()));
            }
            StateSnapshot<AfterSalesState> snapshot = currentSnapshot(threadId);
            return toResult(threadId, snapshot.state(), snapshot.next());
        }
        catch (RuntimeException exception) {
            throw new WorkflowExecutionException("工作流执行失败：" + rootMessage(exception), exception);
        }
    }

    private StateSnapshot<AfterSalesState> currentSnapshot(String threadId) {
        RunnableConfig config = runnableConfig(threadId);
        return workflow.graph().stateOf(config)
                .orElseThrow(() -> new IllegalArgumentException("找不到工作流 checkpoint：" + threadId));
    }

    /** start、stateOf、updateState 和 resume 始终携带同一个并行节点 Executor。 */
    private RunnableConfig runnableConfig(String threadId) {
        return RunnableConfig.builder()
                .threadId(threadId)
                .addParallelNodeExecutor(AfterSalesWorkflow.PREPARE_REFUND_CHECKS, refundCheckExecutor)
                .build();
    }

    private static void requireNext(StateSnapshot<AfterSalesState> snapshot, String expectedNode) {
        if (!expectedNode.equals(snapshot.next())) {
            throw new IllegalStateException("当前节点不允许该恢复操作，next=" + snapshot.next());
        }
    }

    private static WorkflowExecutionResult toResult(
            String threadId, AfterSalesState state, String nextNode) {
        return new WorkflowExecutionResult(
                threadId,
                state.status(),
                END.equals(nextNode) ? "" : nextNode,
                state.string(AfterSalesState.RESPONSE),
                state.string(AfterSalesState.ERROR),
                state.string(AfterSalesState.ORDER_ID),
                state.string(AfterSalesState.REFUND_TRANSACTION_ID),
                state.trace(),
                state.publicView());
    }

    private static String eventMessage(NodeOutput<AfterSalesState> output) {
        String response = output.state().string(AfterSalesState.RESPONSE);
        return response.isBlank() ? "节点执行完成：" + output.node() : response;
    }

    private static String normalizeThreadId(String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requireThreadId(threadId);
    }

    private static String requireThreadId(String threadId) {
        String value = requireText(threadId, "threadId");
        if (!value.matches("[A-Za-z0-9_-]{1,36}")) {
            throw new IllegalArgumentException("threadId 只能包含字母、数字、_ 或 -，且最长 36 位");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        return value.trim();
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private <T> T locked(String threadId, CheckedSupplier<T> supplier) {
        ReentrantLock lock = threadLocks.computeIfAbsent(threadId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return supplier.get();
        }
        catch (RuntimeException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new WorkflowExecutionException(exception.getMessage(), exception);
        }
        finally {
            lock.unlock();
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
