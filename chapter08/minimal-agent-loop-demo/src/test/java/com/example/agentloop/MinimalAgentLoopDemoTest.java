package com.example.agentloop;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinimalAgentLoopDemoTest {
    @Test void runsUntilModelReturnsFinish() {
        var tools = new MinimalAgentLoopDemo.ToolRegistry().register("query_order", id -> "amount=299").register("calculate_refund", amount -> "refund=299");
        var result = new MinimalAgentLoopDemo.AgentLoop(new MinimalAgentLoopDemo.ScriptedModel(), tools, 5).run("订单 A100 退款");
        assertEquals(3, result.steps());
        assertTrue(result.answer().contains("299"));
    }

    @Test void stopsAnEndlessModelAtConfiguredLimit() {
        var tools = new MinimalAgentLoopDemo.ToolRegistry().register("noop", value -> value);
        var endless = (MinimalAgentLoopDemo.DecisionModel) (goal, trace) -> new MinimalAgentLoopDemo.CallTool("noop", "again");
        assertThrows(IllegalStateException.class, () -> new MinimalAgentLoopDemo.AgentLoop(endless, tools, 2).run("永不结束"));
    }

    @org.junit.jupiter.api.Test void observesActualToolAmountAndRefusesUnknownOrder() {
        var tools=new MinimalAgentLoopDemo.ToolRegistry().register("query_order",id -> "A100".equals(id)?"status=PAID,amount=123":"NOT_FOUND")
            .register("calculate_refund",amount -> "refund="+amount);
        var loop=new MinimalAgentLoopDemo.AgentLoop(new MinimalAgentLoopDemo.ScriptedModel(),tools,5);
        org.junit.jupiter.api.Assertions.assertEquals("订单 A100 可退款 123 元",loop.run("订单 A100").answer());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,() -> loop.run("订单 A999"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,() -> new MinimalAgentLoopDemo.CallTool("", "123"));
    }
}
