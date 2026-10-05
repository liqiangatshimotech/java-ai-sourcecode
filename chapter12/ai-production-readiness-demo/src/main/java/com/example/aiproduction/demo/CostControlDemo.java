package com.example.aiproduction.demo;

import com.example.aiproduction.cost.CostCalculator;
import com.example.aiproduction.cost.ModelRouter;

/**
 * 12.2 成本控制例子。
 */
public class CostControlDemo {

    public static void main(String[] args) {
        CostCalculator calculator = new CostCalculator();
        var usage = calculator.estimateTokenUsage("请总结这段客服工单，并判断是否需要人工复核。", 800);
        var route = new ModelRouter().route("普通摘要", usage, false);
        var estimate = calculator.estimate(route.selectedModel(), usage);

        System.out.println(route);
        System.out.println(estimate);
    }
}
