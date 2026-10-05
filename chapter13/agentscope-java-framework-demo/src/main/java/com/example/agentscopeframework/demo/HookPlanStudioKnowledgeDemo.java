package com.example.agentscopeframework.demo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识点 13.7：Hook、Plan 和 Studio。
 *
 * <p>Hook 用来监听执行过程，Plan 用来拆解任务，Studio 用来看 Trace。这个例子用本地事件流模拟一次工单处理的可观测记录。
 */
public class HookPlanStudioKnowledgeDemo {

    public static void main(String[] args) {
        AgentTrace trace = new AgentTrace("trace-ticket-001");
        trace.add("pre_call", "开始处理客服工单");
        trace.add("plan_created", "子任务：查订单、查政策、判断是否升级人工");
        trace.add("tool_call", "query_order_status(ORDER-2002)");
        trace.add("tool_call", "search_policy(refund)");
        trace.add("post_call", "生成 TicketDecision");

        trace.events().forEach(System.out::println);
    }

    public record TraceEvent(String traceId, String type, String detail, Instant at) {}

    public static class AgentTrace {
        private final String traceId;
        private final List<TraceEvent> events = new ArrayList<>();

        public AgentTrace(String traceId) {
            this.traceId = traceId;
        }

        public void add(String type, String detail) {
            events.add(new TraceEvent(traceId, type, detail, Instant.now()));
        }

        public List<TraceEvent> events() {
            return List.copyOf(events);
        }
    }
}
