package com.example.aiproduction.eval;

import java.util.List;

/**
 * 一批评测用例的汇总结果。
 *
 * <p>passRate 是上线门禁里最常用的指标之一。生产系统里可以按 RAG、Tool Calling、Agent Workflow 分组统计。
 */
public record EvaluationSummary(List<EvaluationResult> results, double passRate) {}
