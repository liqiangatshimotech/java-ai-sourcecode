package com.example.enterpriseopsagent.domain;

/**
 * Agent 输出的结构化排障报告。
 *
 * 企业系统不要只保存一段自然语言回答。结构化字段可以继续进入审批、
 * 工单、评测和审计流程。
 */
public record DiagnosisReport(
    String alertId,
    String summary,
    String rootCauseHypothesis,
    String evidence,
    String recommendation,
    boolean requiresApproval
) {
}
