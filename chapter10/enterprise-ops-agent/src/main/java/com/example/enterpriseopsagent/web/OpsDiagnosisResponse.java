package com.example.enterpriseopsagent.web;

import com.example.enterpriseopsagent.config.ModelSettings;
import com.example.enterpriseopsagent.domain.DiagnosisReport;

import java.time.Instant;

/**
 * HTTP 层返回给调用方的排障报告。
 *
 * 除了模型生成的业务结论，这里还返回模型供应商和生成时间，
 * 方便调用方做追踪、审计和问题回放。
 */
public record OpsDiagnosisResponse(
    String alertId,
    String summary,
    String rootCauseHypothesis,
    String evidence,
    String recommendation,
    boolean requiresApproval,
    String modelProvider,
    String modelName,
    Instant generatedAt
) {
    public static OpsDiagnosisResponse from(
        DiagnosisReport report,
        ModelSettings modelSettings,
        Instant generatedAt
    ) {
        return new OpsDiagnosisResponse(
            report.alertId(),
            report.summary(),
            report.rootCauseHypothesis(),
            report.evidence(),
            report.recommendation(),
            report.requiresApproval(),
            modelSettings.provider().name(),
            modelSettings.modelName(),
            generatedAt
        );
    }
}
