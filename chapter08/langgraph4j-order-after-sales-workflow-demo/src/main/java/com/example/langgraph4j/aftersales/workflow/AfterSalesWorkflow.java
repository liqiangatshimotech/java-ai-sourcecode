package com.example.langgraph4j.aftersales.workflow;

import com.example.langgraph4j.aftersales.ai.AfterSalesAssistant;
import com.example.langgraph4j.aftersales.config.WorkflowProperties;
import com.example.langgraph4j.aftersales.domain.*;
import com.example.langgraph4j.aftersales.service.*;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 用 LangGraph4j 1.8.23 定义的订单售后显式状态图。
 *
 * <p>所有节点都返回“局部状态”，LangGraph4j 再依据 schema reducer 合并。
 * 退款政策与风险从 prepare_refund_checks 分叉并行运行，在 evaluate_refund 汇合。</p>
 */
@Component
public class AfterSalesWorkflow {

    public static final String UNDERSTAND_REQUEST = "understand_request";
    public static final String ASK_FOR_INFORMATION = "ask_for_information";
    public static final String WAIT_FOR_INFORMATION = "wait_for_information";
    public static final String LOAD_ORDER = "load_order";
    public static final String ANSWER_CONSULTATION = "answer_consultation";
    public static final String PREPARE_REFUND_CHECKS = "prepare_refund_checks";
    public static final String POLICY_CHECK = "policy_check";
    public static final String RISK_CHECK = "risk_check";
    public static final String EVALUATE_REFUND = "evaluate_refund";
    public static final String HUMAN_APPROVAL = "human_approval";
    public static final String EXECUTE_REFUND = "execute_refund";
    public static final String BUILD_RESPONSE = "build_response";
    public static final String FAILURE = "failure";

    private final AfterSalesAssistant assistant;
    private final OrderQueryService orderQueryService;
    private final RefundPolicyService policyService;
    private final RefundRiskService riskService;
    private final RefundExecutionService refundService;
    private final WorkflowProperties properties;
    private final CompiledGraph<AfterSalesState> graph;

    public AfterSalesWorkflow(
            AfterSalesAssistant assistant,
            OrderQueryService orderQueryService,
            RefundPolicyService policyService,
            RefundRiskService riskService,
            RefundExecutionService refundService,
            WorkflowProperties properties,
            BaseCheckpointSaver checkpointSaver) throws Exception {
        this.assistant = assistant;
        this.orderQueryService = orderQueryService;
        this.policyService = policyService;
        this.riskService = riskService;
        this.refundService = refundService;
        this.properties = properties;
        this.graph = build(checkpointSaver);
    }

    public CompiledGraph<AfterSalesState> graph() {
        return graph;
    }

    public String mermaid() {
        return graph.getGraph(GraphRepresentation.Type.MERMAID,
                "Order after-sales workflow", true).content();
    }

    private CompiledGraph<AfterSalesState> build(BaseCheckpointSaver checkpointSaver) throws Exception {
        StateGraph<AfterSalesState> definition = new StateGraph<>(AfterSalesState.SCHEMA, AfterSalesState::new)
                .addNode(UNDERSTAND_REQUEST, safe(UNDERSTAND_REQUEST, this::understandRequest))
                .addNode(ASK_FOR_INFORMATION, safe(ASK_FOR_INFORMATION, this::askForInformation))
                .addNode(WAIT_FOR_INFORMATION, safe(WAIT_FOR_INFORMATION, this::receiveInformation))
                .addNode(LOAD_ORDER, safe(LOAD_ORDER, this::loadOrder))
                .addNode(ANSWER_CONSULTATION, safe(ANSWER_CONSULTATION, this::answerConsultation))
                .addNode(PREPARE_REFUND_CHECKS, safe(PREPARE_REFUND_CHECKS, this::prepareRefundChecks))
                .addNode(POLICY_CHECK, safe(POLICY_CHECK, this::policyCheck))
                .addNode(RISK_CHECK, safe(RISK_CHECK, this::riskCheck))
                .addNode(EVALUATE_REFUND, safe(EVALUATE_REFUND, this::evaluateRefund))
                .addNode(HUMAN_APPROVAL, safe(HUMAN_APPROVAL, this::receiveApproval))
                .addNode(EXECUTE_REFUND, safe(EXECUTE_REFUND, this::executeRefund))
                .addNode(BUILD_RESPONSE, safe(BUILD_RESPONSE, this::buildResponse))
                .addNode(FAILURE, node_async(this::failure))

                .addEdge(START, UNDERSTAND_REQUEST)
                .addConditionalEdges(UNDERSTAND_REQUEST, edge_async(this::routeAfterUnderstanding), Map.of(
                        "missing", ASK_FOR_INFORMATION,
                        "ready", LOAD_ORDER,
                        "failed", FAILURE))
                .addEdge(ASK_FOR_INFORMATION, WAIT_FOR_INFORMATION)
                .addEdge(WAIT_FOR_INFORMATION, UNDERSTAND_REQUEST)
                .addConditionalEdges(LOAD_ORDER, edge_async(this::routeAfterOrderLoaded), Map.of(
                        "consultation", ANSWER_CONSULTATION,
                        "refund", PREPARE_REFUND_CHECKS,
                        "failed", FAILURE))
                .addEdge(ANSWER_CONSULTATION, BUILD_RESPONSE)

                // 一个源节点连接两个目标即形成 fork；两个分支再连接同一节点完成 join。
                .addEdge(PREPARE_REFUND_CHECKS, POLICY_CHECK)
                .addEdge(PREPARE_REFUND_CHECKS, RISK_CHECK)
                .addEdge(POLICY_CHECK, EVALUATE_REFUND)
                .addEdge(RISK_CHECK, EVALUATE_REFUND)
                .addConditionalEdges(EVALUATE_REFUND, edge_async(this::routeAfterEvaluation), Map.of(
                        "approval", HUMAN_APPROVAL,
                        "execute", EXECUTE_REFUND,
                        "respond", BUILD_RESPONSE,
                        "failed", FAILURE))
                .addConditionalEdges(HUMAN_APPROVAL, edge_async(this::routeAfterApproval), Map.of(
                        "approved", EXECUTE_REFUND,
                        "rejected", BUILD_RESPONSE,
                        "failed", FAILURE))
                .addEdge(EXECUTE_REFUND, BUILD_RESPONSE)
                .addConditionalEdges(BUILD_RESPONSE, edge_async(state -> state.failed() ? "failed" : "done"), Map.of(
                        "failed", FAILURE,
                        "done", END))
                .addEdge(FAILURE, END);

        CompileConfig compileConfig = CompileConfig.builder()
                .checkpointSaver(checkpointSaver)
                .interruptBefore(WAIT_FOR_INFORMATION, HUMAN_APPROVAL)
                // false 表示完成后仍保留历史，便于审计和状态查询。
                .releaseThread(false)
                .recursionLimit(properties.getRecursionLimit())
                .build();
        return definition.compile(compileConfig);
    }

    private Map<String, Object> understandRequest(AfterSalesState state) {
        IntentAnalysis analysis = assistant.analyzeRequest(
                state.string(AfterSalesState.USER_MESSAGE), state.string(AfterSalesState.ORDER_ID));
        // 补充订单号时，用户往往只回复“ORD-1001”。这时应保留上一轮
        // 已确认的退款意图，而不是把短消息错判成普通咨询。
        AfterSalesIntent effectiveIntent = state.integer(AfterSalesState.CLARIFICATION_ROUNDS) > 0
                && state.intent() == AfterSalesIntent.REFUND
                && analysis.intent() != AfterSalesIntent.REFUND
                ? AfterSalesIntent.REFUND : analysis.intent();
        if (analysis.needsMoreInformation()
                && state.integer(AfterSalesState.CLARIFICATION_ROUNDS) >= properties.getMaxClarificationRounds()) {
            return failureUpdate(UNDERSTAND_REQUEST, "补充信息次数已达上限");
        }
        return Map.of(
                AfterSalesState.INTENT, effectiveIntent.name(),
                AfterSalesState.ORDER_ID, analysis.orderId(),
                AfterSalesState.MISSING_FIELDS, analysis.missingFields(),
                AfterSalesState.STATUS, WorkflowStatus.RUNNING.name(),
                AfterSalesState.TRACE, "understand_request:intent=" + effectiveIntent);
    }

    private Map<String, Object> askForInformation(AfterSalesState state) {
        return Map.of(
                AfterSalesState.STATUS, WorkflowStatus.WAITING_FOR_INFORMATION.name(),
                AfterSalesState.RESPONSE, "请补充订单号（例如 ORD-1001），我会从当前 checkpoint 继续处理。",
                AfterSalesState.TRACE, "ask_for_information:checkpoint-before-wait");
    }

    private Map<String, Object> receiveInformation(AfterSalesState state) {
        return Map.of(
                AfterSalesState.CLARIFICATION_ROUNDS,
                state.integer(AfterSalesState.CLARIFICATION_ROUNDS) + 1,
                AfterSalesState.STATUS, WorkflowStatus.RUNNING.name(),
                AfterSalesState.RESPONSE, "",
                AfterSalesState.TRACE, "wait_for_information:resumed");
    }

    private Map<String, Object> loadOrder(AfterSalesState state) {
        OrderSnapshot order = orderQueryService.findRequired(state.string(AfterSalesState.ORDER_ID));
        return Map.ofEntries(
                Map.entry(AfterSalesState.PRODUCT_NAME, order.productName()),
                Map.entry(AfterSalesState.PAID_AMOUNT, order.paidAmount().toPlainString()),
                Map.entry(AfterSalesState.DELIVERED_DATE, order.deliveredDate().toString()),
                Map.entry(AfterSalesState.ALREADY_REFUNDED, order.alreadyRefunded()),
                Map.entry(AfterSalesState.SUSPICIOUS_ACCOUNT, order.suspiciousAccount()),
                Map.entry(AfterSalesState.TRACE, "load_order:" + order.orderId()));
    }

    private Map<String, Object> answerConsultation(AfterSalesState state) {
        String answer = assistant.answerConsultation(
                state.string(AfterSalesState.USER_MESSAGE), state.order());
        return Map.of(
                AfterSalesState.RESPONSE, answer,
                AfterSalesState.STATUS, WorkflowStatus.COMPLETED.name(),
                AfterSalesState.TRACE, "answer_consultation:completed");
    }

    private Map<String, Object> prepareRefundChecks(AfterSalesState state) {
        return Map.of(
                AfterSalesState.TRACE, "prepare_refund_checks:fork-policy-and-risk");
    }

    private Map<String, Object> policyCheck(AfterSalesState state) {
        PolicyCheckResult result = policyService.check(state.order());
        return Map.of(
                AfterSalesState.POLICY_ALLOWED, result.allowed(),
                AfterSalesState.POLICY_REASON, result.reason(),
                AfterSalesState.TRACE, "policy_check:" + result.allowed());
    }

    private Map<String, Object> riskCheck(AfterSalesState state) {
        RiskCheckResult result = riskService.check(state.order());
        return Map.of(
                AfterSalesState.RISK_LEVEL, result.level().name(),
                AfterSalesState.RISK_REASON, result.reason(),
                AfterSalesState.TRACE, "risk_check:" + result.level());
    }

    private Map<String, Object> evaluateRefund(AfterSalesState state) {
        if (state.failed()) {
            return Map.of(AfterSalesState.TRACE, "evaluate_refund:upstream-failed");
        }
        if (!state.bool(AfterSalesState.POLICY_ALLOWED)) {
            return Map.of(
                    AfterSalesState.REFUND_DECISION, "DENIED_POLICY",
                    AfterSalesState.DECISION_DETAIL, state.string(AfterSalesState.POLICY_REASON),
                    AfterSalesState.TRACE, "evaluate_refund:denied-policy");
        }
        if (state.riskLevel() == RiskLevel.HIGH) {
            return Map.of(
                    AfterSalesState.REFUND_DECISION, "DENIED_RISK",
                    AfterSalesState.DECISION_DETAIL, state.string(AfterSalesState.RISK_REASON),
                    AfterSalesState.TRACE, "evaluate_refund:denied-risk");
        }
        if (state.riskLevel() == RiskLevel.MEDIUM) {
            return Map.of(
                    AfterSalesState.REFUND_DECISION, "WAITING_APPROVAL",
                    AfterSalesState.APPROVAL_DECISION, ApprovalDecision.PENDING.name(),
                    AfterSalesState.STATUS, WorkflowStatus.WAITING_FOR_APPROVAL.name(),
                    AfterSalesState.DECISION_DETAIL, state.string(AfterSalesState.RISK_REASON),
                    AfterSalesState.TRACE, "evaluate_refund:checkpoint-before-approval");
        }
        return Map.of(
                AfterSalesState.REFUND_DECISION, "AUTO_APPROVED",
                AfterSalesState.DECISION_DETAIL, "政策允许且风险较低",
                AfterSalesState.TRACE, "evaluate_refund:auto-approved");
    }

    private Map<String, Object> receiveApproval(AfterSalesState state) {
        if (state.approvalDecision() == ApprovalDecision.PENDING) {
            return failureUpdate(HUMAN_APPROVAL, "恢复工作流时未提供审批结论");
        }
        return Map.of(
                AfterSalesState.STATUS, WorkflowStatus.RUNNING.name(),
                AfterSalesState.TRACE, "human_approval:" + state.approvalDecision());
    }

    private Map<String, Object> executeRefund(AfterSalesState state) {
        RefundReceipt receipt = refundService.refund(
                "after-sales:" + state.string(AfterSalesState.THREAD_ID), state.order());
        return Map.of(
                AfterSalesState.REFUND_TRANSACTION_ID, receipt.transactionId(),
                AfterSalesState.REFUND_DECISION, "REFUNDED",
                AfterSalesState.DECISION_DETAIL, "退款金额 " + receipt.amount() + " 元",
                AfterSalesState.TRACE, "execute_refund:" + receipt.transactionId());
    }

    private Map<String, Object> buildResponse(AfterSalesState state) {
        if (state.failed()) {
            return failure(state);
        }
        // 咨询节点已生成回复，这里只补齐终态。
        if (state.intent() == AfterSalesIntent.CONSULTATION && !state.string(AfterSalesState.RESPONSE).isBlank()) {
            return Map.of(AfterSalesState.STATUS, WorkflowStatus.COMPLETED.name(),
                    AfterSalesState.TRACE, "build_response:consultation");
        }
        String answer = assistant.explainRefundResult(
                state.string(AfterSalesState.REFUND_DECISION), state.order(),
                state.string(AfterSalesState.DECISION_DETAIL));
        return Map.of(
                AfterSalesState.RESPONSE, answer,
                AfterSalesState.STATUS, WorkflowStatus.COMPLETED.name(),
                AfterSalesState.TRACE, "build_response:refund");
    }

    private Map<String, Object> failure(AfterSalesState state) {
        String error = state.string(AfterSalesState.ERROR);
        return Map.of(
                AfterSalesState.STATUS, WorkflowStatus.FAILED.name(),
                AfterSalesState.RESPONSE, "工作流未能完成：" + error,
                AfterSalesState.TRACE, "failure:" + state.string(AfterSalesState.FAILED_NODE));
    }

    private String routeAfterUnderstanding(AfterSalesState state) {
        if (state.failed()) return "failed";
        Object missing = state.value(AfterSalesState.MISSING_FIELDS).orElse(List.of());
        return missing instanceof List<?> list && !list.isEmpty() ? "missing" : "ready";
    }

    private String routeAfterOrderLoaded(AfterSalesState state) {
        if (state.failed()) return "failed";
        return state.intent() == AfterSalesIntent.REFUND ? "refund" : "consultation";
    }

    private String routeAfterEvaluation(AfterSalesState state) {
        if (state.failed()) return "failed";
        return switch (state.string(AfterSalesState.REFUND_DECISION)) {
            case "WAITING_APPROVAL" -> "approval";
            case "AUTO_APPROVED" -> "execute";
            default -> "respond";
        };
    }

    private String routeAfterApproval(AfterSalesState state) {
        if (state.failed()) return "failed";
        return state.approvalDecision() == ApprovalDecision.APPROVED ? "approved" : "rejected";
    }

    private AsyncNodeAction<AfterSalesState> safe(String nodeId, NodeAction<AfterSalesState> action) {
        return node_async(state -> {
            if (state.failed()) {
                return Map.of(AfterSalesState.TRACE, nodeId + ":skipped-after-failure");
            }
            try {
                return action.apply(state);
            }
            catch (Exception exception) {
                return failureUpdate(nodeId, rootMessage(exception));
            }
        });
    }

    private static Map<String, Object> failureUpdate(String nodeId, String message) {
        return Map.of(
                AfterSalesState.ERROR, message,
                AfterSalesState.FAILED_NODE, nodeId,
                AfterSalesState.STATUS, WorkflowStatus.FAILED.name(),
                AfterSalesState.TRACE, nodeId + ":failed");
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
