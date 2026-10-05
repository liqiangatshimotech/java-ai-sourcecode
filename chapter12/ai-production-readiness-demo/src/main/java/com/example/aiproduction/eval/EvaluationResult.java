package com.example.aiproduction.eval;

/**
 * 单条评测结果。
 *
 * <p>passed 只表示这条用例是否通过自动规则。真实项目里还可以增加人工评分和 LLM-as-Judge 分数，但这些分数要保留审计样本。
 */
public record EvaluationResult(String caseId, boolean passed, String reason) {}
