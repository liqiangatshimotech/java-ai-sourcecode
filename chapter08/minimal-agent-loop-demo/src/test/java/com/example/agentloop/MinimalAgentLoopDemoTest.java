package com.example.agentloop;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinimalAgentLoopDemoTest {
    @Test void runsUntilModelReturnsFinish() {
        var tools = new MinimalAgentLoopDemo.ToolRegistry().register("query_order", id -> "amount=299").register("calculate_refund", amount -> "refund=299");
        var result = new MinimalAgentLoopDemo.AgentLoop(new MinimalAgentLoopDemo.ScriptedModel(), tools, 5).run("退款");
        assertEquals(3, result.steps());
        assertTrue(result.answer().contains("299"));
    }

    @Test void stopsAnEndlessModelAtConfiguredLimit() {
        var tools = new MinimalAgentLoopDemo.ToolRegistry().register("noop", value -> value);
        var endless = (MinimalAgentLoopDemo.DecisionModel) (goal, trace) -> new MinimalAgentLoopDemo.CallTool("noop", "again");
        assertThrows(IllegalStateException.class, () -> new MinimalAgentLoopDemo.AgentLoop(endless, tools, 2).run("永不结束"));
    }
}
