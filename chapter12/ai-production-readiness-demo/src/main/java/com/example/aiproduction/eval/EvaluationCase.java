package com.example.aiproduction.eval;

/**
 * 一条离线评测用例。
 *
 * <p>expectedTool 是为了评测 Tool Calling 或 Agent 是否选对工具。expectedAnswerKeyword 用来做简单的答案命中检查。
 */
public record EvaluationCase(
        String id, String question, String expectedTool, String expectedAnswerKeyword, boolean shouldEscalate) {}
