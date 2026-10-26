package com.example.agentconcepts;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgentConceptsDemoTest {
    @Test void smallOrderCanBeHandledAutomaticallyAndLeavesTrace() {
        var memory = new AgentConceptsDemo.Memory();
        var agent = new AgentConceptsDemo.Agent(new AgentConceptsDemo.RuleBasedModel(), new AgentConceptsDemo.OrderTool(), memory, 500);
        assertTrue(agent.handle("查询 A100 并退款").contains("可自动退款"));
        assertEquals(2, agent.memory().size());
    }

    @Test void businessGuardrailEscalatesLargeOrder() {
        var agent = new AgentConceptsDemo.Agent(new AgentConceptsDemo.RuleBasedModel(), new AgentConceptsDemo.OrderTool(), new AgentConceptsDemo.Memory(), 500);
        assertTrue(agent.handle("查询 A200 并退款").contains("转人工审批"));
    }

    @org.junit.jupiter.api.Test void neverDefaultsMissingOrUnknownOrderAndRedactsKey() {
        var model=new AgentConceptsDemo.RuleBasedModel();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,() -> model.decide("查询订单",java.util.List.of()));
        org.junit.jupiter.api.Assertions.assertEquals("A999",model.decide("订单 A999",java.util.List.of()).argument());
        org.junit.jupiter.api.Assertions.assertFalse(new AgentConceptsDemo.ModelSettings("deepseek","url","model","test-secret").toString().contains("test-secret"));
    }
}
