package com.example.aiproduction.demo;

import com.example.aiproduction.eval.EvaluationCase;
import com.example.aiproduction.eval.EvaluationRunner;
import com.example.aiproduction.eval.EvaluationRunner.DemoAgentAnswer;
import java.util.List;

/**
 * 12.4 评测体系例子。
 */
public class EvaluationDemo {

    public static void main(String[] args) {
        var cases =
                List.of(
                        new EvaluationCase(
                                "case-001",
                                "ORDER-2002 一直不发货，能退款吗？",
                                "order.query",
                                "库存不足",
                                true));
        var answer = new DemoAgentAnswer("order.query", "订单库存不足，需要人工复核退款诉求。", true);

        System.out.println(new EvaluationRunner().run(cases, answer));
    }
}
