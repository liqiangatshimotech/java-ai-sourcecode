package com.example.agenticworkflow;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgenticWorkflowDemoTest {
    @Test
    void workflowRunsParallelFactsAndReplyQualityLoop() {
        var tools = new AgenticWorkflowDemo.AfterSalesTools();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var workflow = new AgenticWorkflowDemo.Workflow(
                    new AgenticWorkflowDemo.RuleIntentAgent(),
                    new AgenticWorkflowDemo.ToolOrderAgent(tools),
                    new AgenticWorkflowDemo.ToolLogisticsAgent(tools),
                    new AgenticWorkflowDemo.RuleCompensationAgent(),
                    new AgenticWorkflowDemo.TemplateReplyAgent(),
                    new AgenticWorkflowDemo.RuleReplyQualityAgent(),
                    executor,
                    2
            );

            var result = workflow.run(new AgenticWorkflowDemo.Request("A100", "物流没送到，需要补偿"));

            assertTrue(result.reply().contains("人工确认"));
            assertEquals("SUCCEEDED", result.invocations().getFirst().status());
            assertEquals(2, result.invocations().stream()
                    .filter(invocation -> invocation.agentName().equals("PlainJavaReplyAgent"))
                    .count());
            assertTrue(result.scope().containsKey("fact.orderStatus"));
            assertTrue(result.scope().containsKey("fact.logisticsStatus"));
        }
    }

    @Test
    void lowConfidenceStopsBeforeFactAgents() {
        var tools = new AgenticWorkflowDemo.AfterSalesTools();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var workflow = new AgenticWorkflowDemo.Workflow(
                    new AgenticWorkflowDemo.RuleIntentAgent(),
                    new AgenticWorkflowDemo.ToolOrderAgent(tools),
                    new AgenticWorkflowDemo.ToolLogisticsAgent(tools),
                    new AgenticWorkflowDemo.RuleCompensationAgent(),
                    new AgenticWorkflowDemo.TemplateReplyAgent(),
                    new AgenticWorkflowDemo.RuleReplyQualityAgent(),
                    executor,
                    2
            );

            var result = workflow.run(new AgenticWorkflowDemo.Request("A100", "帮我看看"));

            assertTrue(result.reply().contains("人工"));
            assertEquals(1, result.invocations().size());
        }
    }

    @Test
    void defaultsToDeepSeekAndCanSwitchToOllama() {
        assertEquals("deepseek", AgenticWorkflowDemo.ModelSettings.fromEnvironment(Map.of()).provider());
        assertEquals("ollama", AgenticWorkflowDemo.ModelSettings.fromEnvironment(
                Map.of("AI_PROVIDER", "ollama")
        ).provider());
    }
}
