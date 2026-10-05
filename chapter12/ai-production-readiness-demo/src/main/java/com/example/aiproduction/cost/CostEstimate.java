package com.example.aiproduction.cost;

/**
 * 成本估算结果。
 *
 * <p>金额单位使用美元只是为了演示计算方式。真实项目可以换成人民币、内部点数或预算额度。
 */
public record CostEstimate(String modelName, TokenUsage usage, double estimatedUsd) {

    public boolean exceeds(double budgetUsd) {
        return estimatedUsd > budgetUsd;
    }
}
