package com.example.aiproduction.cost;

/**
 * 模型调用成本估算器。
 *
 * <p>这里刻意不调用任何模型 API，而是把“估算输入输出 Token”和“按模型单价计算成本”拆开。生产系统可以在请求前做预算预估，
 * 请求后再用模型返回的真实 usage 做账单校正。
 */
public class CostCalculator {

    public TokenUsage estimateTokenUsage(String prompt, int expectedOutputTokens) {
        int inputTokens = Math.max(1, prompt.length() / 4);
        return new TokenUsage(inputTokens, expectedOutputTokens);
    }

    public CostEstimate estimate(ModelProfile profile, TokenUsage usage) {
        double inputCost = usage.inputTokens() / 1_000_000.0 * profile.inputPricePerMillionTokens();
        double outputCost = usage.outputTokens() / 1_000_000.0 * profile.outputPricePerMillionTokens();
        return new CostEstimate(profile.modelName(), usage, inputCost + outputCost);
    }
}
