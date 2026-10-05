package com.example.aiproduction.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aiproduction.eval.EvaluationRunner.DemoAgentAnswer;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvaluationRunnerTest {

    @Test
    void calculatesPassRate() {
        var cases =
                List.of(
                        new EvaluationCase("case-1", "退款问题", "order.query", "库存不足", true),
                        new EvaluationCase("case-2", "退款问题", "order.query", "库存不足", true));
        var answer = new DemoAgentAnswer("order.query", "订单库存不足，需要人工复核。", true);

        EvaluationSummary summary = new EvaluationRunner().run(cases, answer);

        assertEquals(1.0, summary.passRate());
        assertTrue(summary.results().stream().allMatch(EvaluationResult::passed));
    }
}
