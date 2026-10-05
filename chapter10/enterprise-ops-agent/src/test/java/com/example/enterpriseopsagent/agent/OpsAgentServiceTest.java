package com.example.enterpriseopsagent.agent;

import com.example.enterpriseopsagent.domain.DiagnosisReport;
import com.example.enterpriseopsagent.domain.OpsAlert;
import com.example.enterpriseopsagent.tool.InMemoryOpsServices;
import com.example.enterpriseopsagent.tool.OpsTools;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpsAgentServiceTest {

    @Test
    void shouldGenerateDiagnosisReportAndRecordAuditLog() {
        OpsTools tools = new OpsTools(
            InMemoryOpsServices.logs(),
            InMemoryOpsServices.metrics(),
            InMemoryOpsServices.deployments(),
            InMemoryOpsServices.approvals()
        );
        AtomicReference<DiagnosisReport> auditedReport = new AtomicReference<>();

        OpsAgentService service = new OpsAgentService(
            (serviceName, description) -> "Runbook: 先检查最近发布，再检查下游超时。",
            new OpsToolExecutor(tools),
            new RuleBasedDiagnosisModel(),
            (alert, report) -> auditedReport.set(report)
        );

        OpsAlert alert = new OpsAlert(
            "ALERT-001",
            "payment-service",
            "prod",
            "P1",
            "支付服务错误率升高",
            "支付回调接口出现大量 timeout。",
            Instant.now()
        );

        DiagnosisReport report = service.diagnose(alert);

        assertEquals("ALERT-001", report.alertId());
        assertTrue(report.requiresApproval());
        assertTrue(report.evidence().contains("Runbook"));
        assertTrue(report.evidence().contains("发布证据"));
        assertNotNull(auditedReport.get());
        assertEquals(report, auditedReport.get());
    }
}
