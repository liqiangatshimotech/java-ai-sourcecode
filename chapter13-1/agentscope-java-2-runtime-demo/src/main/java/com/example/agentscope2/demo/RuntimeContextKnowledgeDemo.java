package com.example.agentscope2.demo;

import io.agentscope.core.agent.RuntimeContext;

/**
 * 展示 2.0 里每次调用都应该显式传递的运行上下文。
 */
public class RuntimeContextKnowledgeDemo {

    public static void main(String[] args) {
        RuntimeContext context = RuntimeContext.builder()
                .userId("alice")
                .sessionId("ticket-demo")
                .put("tenantId", "tenant-a")
                .put("traceId", "trace-20260708-001")
                .build();

        System.out.println("userId=" + context.getUserId());
        System.out.println("sessionId=" + context.getSessionId());
        System.out.println("tenantId=" + context.get("tenantId"));
        System.out.println("traceId=" + context.get("traceId"));
    }
}
