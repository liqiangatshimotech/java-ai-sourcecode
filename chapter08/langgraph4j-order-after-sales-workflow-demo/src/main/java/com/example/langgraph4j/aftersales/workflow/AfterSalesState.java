package com.example.langgraph4j.aftersales.workflow;

import com.example.langgraph4j.aftersales.domain.AfterSalesIntent;
import com.example.langgraph4j.aftersales.domain.ApprovalDecision;
import com.example.langgraph4j.aftersales.domain.OrderSnapshot;
import com.example.langgraph4j.aftersales.domain.RiskLevel;
import com.example.langgraph4j.aftersales.domain.WorkflowStatus;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 售后图的共享状态。
 *
 * <p>checkpoint 中只保存字符串、数字、布尔和字符串列表，确保 MemorySaver 与
 * MysqlSaver 重新加载后类型一致。trace 使用去重 appender reducer：它既能合并
 * 两个并行检查的轨迹，又能避免 LangGraph4j ParallelNode 将 fork 前已有的完整
 * state 交给外层再合并时，把同一条轨迹重复追加。</p>
 */
public class AfterSalesState extends AgentState {

    public static final String THREAD_ID = "threadId";
    public static final String USER_MESSAGE = "userMessage";
    public static final String INTENT = "intent";
    public static final String ORDER_ID = "orderId";
    public static final String MISSING_FIELDS = "missingFields";
    public static final String CLARIFICATION_ROUNDS = "clarificationRounds";
    public static final String STATUS = "status";
    public static final String RESPONSE = "response";
    public static final String ERROR = "error";
    public static final String FAILED_NODE = "failedNode";
    public static final String TRACE = "trace";

    public static final String PRODUCT_NAME = "productName";
    public static final String PAID_AMOUNT = "paidAmount";
    public static final String DELIVERED_DATE = "deliveredDate";
    public static final String ALREADY_REFUNDED = "alreadyRefunded";
    public static final String SUSPICIOUS_ACCOUNT = "suspiciousAccount";

    public static final String POLICY_ALLOWED = "policyAllowed";
    public static final String POLICY_REASON = "policyReason";
    public static final String RISK_LEVEL = "riskLevel";
    public static final String RISK_REASON = "riskReason";
    public static final String REFUND_DECISION = "refundDecision";
    public static final String DECISION_DETAIL = "decisionDetail";
    public static final String APPROVAL_DECISION = "approvalDecision";
    public static final String APPROVER = "approver";
    public static final String REFUND_TRANSACTION_ID = "refundTransactionId";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            TRACE, Channels.appender(ArrayList::new)
    );

    public AfterSalesState(Map<String, Object> initData) {
        super(initData);
    }

    public String string(String key) {
        return this.<Object>value(key).map(Object::toString).orElse("");
    }

    public boolean bool(String key) {
        Object value = this.<Object>value(key).orElse(false);
        return value instanceof Boolean flag ? flag : Boolean.parseBoolean(value.toString());
    }

    public int integer(String key) {
        Object value = this.<Object>value(key).orElse(0);
        return value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString());
    }

    public AfterSalesIntent intent() {
        return AfterSalesIntent.valueOf(string(INTENT).isBlank() ? "UNKNOWN" : string(INTENT));
    }

    public WorkflowStatus status() {
        return WorkflowStatus.valueOf(string(STATUS).isBlank() ? "RUNNING" : string(STATUS));
    }

    public ApprovalDecision approvalDecision() {
        return ApprovalDecision.valueOf(string(APPROVAL_DECISION).isBlank()
                ? "PENDING" : string(APPROVAL_DECISION));
    }

    public RiskLevel riskLevel() {
        return RiskLevel.valueOf(string(RISK_LEVEL).isBlank() ? "LOW" : string(RISK_LEVEL));
    }

    public boolean failed() {
        return !string(ERROR).isBlank();
    }

    @SuppressWarnings("unchecked")
    public List<String> trace() {
        return this.<List<Object>>value(TRACE).orElseGet(List::of).stream().map(Object::toString).toList();
    }

    public OrderSnapshot order() {
        if (string(ORDER_ID).isBlank() || string(PAID_AMOUNT).isBlank()) {
            throw new IllegalStateException("状态中还没有订单快照");
        }
        return new OrderSnapshot(
                string(ORDER_ID),
                string(PRODUCT_NAME),
                new BigDecimal(string(PAID_AMOUNT)),
                LocalDate.parse(string(DELIVERED_DATE)),
                bool(ALREADY_REFUNDED),
                bool(SUSPICIOUS_ACCOUNT));
    }

    public Map<String, Object> publicView() {
        return Map.ofEntries(
                Map.entry(THREAD_ID, string(THREAD_ID)),
                Map.entry(STATUS, status().name()),
                Map.entry(INTENT, intent().name()),
                Map.entry(ORDER_ID, string(ORDER_ID)),
                Map.entry(CLARIFICATION_ROUNDS, integer(CLARIFICATION_ROUNDS)),
                Map.entry(POLICY_REASON, string(POLICY_REASON)),
                Map.entry(RISK_LEVEL, string(RISK_LEVEL)),
                Map.entry(RISK_REASON, string(RISK_REASON)),
                Map.entry(REFUND_DECISION, string(REFUND_DECISION)),
                Map.entry(APPROVAL_DECISION, string(APPROVAL_DECISION)),
                Map.entry(REFUND_TRANSACTION_ID, string(REFUND_TRANSACTION_ID)),
                Map.entry(RESPONSE, string(RESPONSE)),
                Map.entry(ERROR, string(ERROR)),
                Map.entry(FAILED_NODE, string(FAILED_NODE)),
                Map.entry(TRACE, trace()));
    }
}
