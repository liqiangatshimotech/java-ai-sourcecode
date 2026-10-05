package com.example.agentruntime.service;

import com.example.agentruntime.config.AgentProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentRuntimeServiceTest {
    private AgentRuntimeService runtime() {
        var properties = new AgentProperties(new AgentProperties.Model("deepseek", "", "", ""),
                new AgentProperties.Runtime(8, 20));
        return new AgentRuntimeService(properties);
    }

    @Test void waitsForApprovalThenCreatesTicket() {
        var runtime = runtime();
        var first = runtime.run(new AgentRuntimeService.AgentRunRequest("session-a", "订单 A100 申请补偿", false));
        assertEquals("WAITING_APPROVAL", first.status());
        assertEquals(5, first.steps().size());
        assertEquals("BLOCKED", first.steps().getLast().status());
        assertEquals(7, runtime.events(first.runId()).size());
        assertTrue(runtime.trace(first.runId()).facts().containsKey("pendingApproval"));
        var approved = runtime.approve(first.runId());
        assertEquals("COMPLETED", approved.status());
        assertEquals("TICKET-A100", approved.facts().get("ticketId"));
        assertEquals(6, approved.steps().size());
        assertThrows(IllegalStateException.class, () -> runtime.approve(first.runId()));
        assertEquals(3, runtime.history("session-a").size());
        assertTrue(runtime.history("session-b").isEmpty());
    }

    @Test void noCompensationWhenDeliveryDelayIsShort() {
        var result = runtime().run(new AgentRuntimeService.AgentRunRequest("s", "查询订单 A200", false));
        assertEquals("COMPLETED", result.status());
        assertEquals(4, result.steps().size());
        assertFalse(result.facts().containsKey("ticketId"));
    }

    @Test void toolExceptionBecomesFailedObservation() {
        var result = runtime().run(new AgentRuntimeService.AgentRunRequest("s", "查询订单 A999", false));
        assertEquals("FAILED", result.status());
        assertEquals("FAILED", result.steps().getFirst().status());
    }
}
