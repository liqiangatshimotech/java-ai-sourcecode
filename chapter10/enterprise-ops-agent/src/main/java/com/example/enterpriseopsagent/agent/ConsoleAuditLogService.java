package com.example.enterpriseopsagent.agent;

import com.example.enterpriseopsagent.domain.DiagnosisReport;
import com.example.enterpriseopsagent.domain.OpsAlert;

/**
 * 控制台审计日志实现。
 *
 * 真实项目里可以替换成数据库、消息队列或 OpenTelemetry Trace。
 */
public class ConsoleAuditLogService implements AuditLogService {

    @Override
    public void recordDiagnosis(OpsAlert alert, DiagnosisReport report) {
        System.out.println("[audit] alertId=" + alert.alertId()
            + ", service=" + alert.serviceName()
            + ", approvalRequired=" + report.requiresApproval());
    }
}
