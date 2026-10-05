package com.example.enterpriseopsagent.agent;

import com.example.enterpriseopsagent.domain.DiagnosisReport;
import com.example.enterpriseopsagent.domain.OpsAlert;

/**
 * 审计日志接口。
 *
 * Agent 的每次输入、工具调用、模型输出和最终建议都应该可追溯。
 * Demo 只记录最终报告，后续可以扩展为 agent_step、tool_call、token_usage 等表。
 */
public interface AuditLogService {
    void recordDiagnosis(OpsAlert alert, DiagnosisReport report);
}
