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
}
