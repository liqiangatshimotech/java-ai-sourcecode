package com.example.agentscopeframework.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.agentscopeframework.project.ServiceTicketProjectAgent;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgeDemoTest {

    @Test
    void ragDemoReturnsPolicyText() {
        RagMcpKnowledgeDemo.PolicyKnowledgeBase rag =
                new RagMcpKnowledgeDemo.InMemoryPolicyKnowledgeBase();

        assertTrue(rag.retrieve("refund").contains("退款"));
    }

    @Test
    void multiAgentDemoClassifiesRefundTicket() {
        MultiAgentKnowledgeDemo.ClassifierAgent classifier =
                new MultiAgentKnowledgeDemo.ClassifierAgent();

        String category = classifier.classify(ServiceTicketProjectAgent.sampleTicket());

        assertEquals("退款/履约", category);
    }

    @Test
    void traceRecordsEvents() {
        HookPlanStudioKnowledgeDemo.AgentTrace trace =
                new HookPlanStudioKnowledgeDemo.AgentTrace("trace-1");

        trace.add("tool_call", "query_order_status");

        assertFalse(trace.events().isEmpty());
        assertEquals("tool_call", trace.events().get(0).type());
    }

    @Test
    void sessionSnapshotKeepsTimeline() {
        var snapshot =
                new MemorySessionStateKnowledgeDemo.TicketSessionSnapshot(
                        "s1", "t1", List.of("start"), java.time.Instant.now());

        assertEquals("s1", snapshot.sessionId());
        assertEquals(List.of("start"), snapshot.timeline());
    }
}
