package com.example.langgraph4j.aftersales.workflow;

import com.example.langgraph4j.aftersales.domain.WorkflowStatus;

import java.util.List;
import java.util.Map;

/** 一次启动或恢复执行后的快照。 */
public record WorkflowExecutionResult(
        String threadId,
        WorkflowStatus status,
        String nextNode,
        String response,
        String error,
        String orderId,
        String refundTransactionId,
        List<String> trace,
        Map<String, Object> state) {
}
