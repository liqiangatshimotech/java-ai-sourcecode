package com.example.langgraph4j.aftersales.workflow;

import com.example.langgraph4j.aftersales.domain.WorkflowStatus;

import java.util.Map;

/** SSE 推送的一条节点事件。 */
public record WorkflowEvent(
        long sequence,
        String threadId,
        String node,
        WorkflowStatus status,
        String message,
        Map<String, Object> state) {
}
