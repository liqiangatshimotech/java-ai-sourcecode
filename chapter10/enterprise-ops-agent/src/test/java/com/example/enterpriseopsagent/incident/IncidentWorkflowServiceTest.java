package com.example.enterpriseopsagent.incident;

import com.example.enterpriseopsagent.agent.OpsAgentService;
import com.example.enterpriseopsagent.agent.OpsToolExecutor;
import com.example.enterpriseopsagent.agent.RuleBasedDiagnosisModel;
import com.example.enterpriseopsagent.domain.DecisionType;
import com.example.enterpriseopsagent.domain.DiagnosisIncident;
import com.example.enterpriseopsagent.domain.IncidentStatus;
import com.example.enterpriseopsagent.domain.OpsAlert;
import com.example.enterpriseopsagent.tool.InMemoryOpsServices;
import com.example.enterpriseopsagent.tool.OpsTools;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncidentWorkflowServiceTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-06-11T07:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldCreateIncidentAndApprovalForP1Alert() {
        IncidentWorkflowService service = newService();

        DiagnosisIncident incident = service.ingestAlert(alert("ALERT-001"));

        assertEquals("INC-TEST-001", incident.incidentId());
        assertEquals(IncidentStatus.WAITING_APPROVAL, incident.status());
        assertEquals("APPROVAL-payment-service-prod-ROLLBACK", incident.approvalTicketId());
        assertEquals(Instant.parse("2026-06-11T07:15:00Z"), incident.slaDeadline());
        assertEquals(3, incident.timeline().size());
        assertFalse(incident.slaBreached(fixedClock.instant()));
    }

    @Test
    void shouldReturnExistingIncidentWhenAlertIsDuplicated() {
        IncidentWorkflowService service = newService();

        DiagnosisIncident firstIncident = service.ingestAlert(alert("ALERT-DUP-001"));
        DiagnosisIncident secondIncident = service.ingestAlert(alert("ALERT-DUP-001"));

        assertEquals(firstIncident.incidentId(), secondIncident.incidentId());
        assertEquals(1, secondIncident.duplicateCount());
        assertEquals(1, service.search(new IncidentQuery(null, "payment-service", "prod")).size());
        assertEquals("DUPLICATE_ALERT_RECEIVED", secondIncident.timeline().get(3).type());
    }

    @Test
    void shouldApproveAndResolveIncident() {
        IncidentWorkflowService service = newService();
        DiagnosisIncident incident = service.ingestAlert(alert("ALERT-APPROVE-001"));

        DiagnosisIncident approvedIncident = service.decide(
            incident.incidentId(),
            DecisionType.APPROVE,
            "sre-zhangsan",
            "同意回滚到上一稳定版本"
        );
        DiagnosisIncident resolvedIncident = service.resolve(
            incident.incidentId(),
            "sre-zhangsan",
            "回滚后错误率恢复到 0.3%"
        );

        assertEquals(IncidentStatus.APPROVED, approvedIncident.status());
        assertEquals(IncidentStatus.RESOLVED, resolvedIncident.status());
        assertEquals(5, resolvedIncident.timeline().size());
    }

    @Test
    void shouldRejectDecisionWhenIncidentDoesNotWaitApproval() {
        IncidentWorkflowService service = newService();
        DiagnosisIncident incident = service.ingestAlert(alert("ALERT-REJECT-001"));
        service.decide(incident.incidentId(), DecisionType.REJECT, "sre-lisi", "证据不足");

        assertThrows(
            IllegalStateException.class,
            () -> service.decide(incident.incidentId(), DecisionType.APPROVE, "sre-lisi", "再次审批")
        );
    }

    private IncidentWorkflowService newService() {
        OpsTools tools = new OpsTools(
            InMemoryOpsServices.logs(),
            InMemoryOpsServices.metrics(),
            InMemoryOpsServices.deployments(),
            InMemoryOpsServices.approvals()
        );
        OpsAgentService opsAgentService = new OpsAgentService(
            (serviceName, description) -> "Runbook: 先检查最近发布，再检查下游超时。",
            new OpsToolExecutor(tools),
            new RuleBasedDiagnosisModel(),
            (alert, report) -> {
            }
        );
        return new IncidentWorkflowService(
            opsAgentService,
            InMemoryOpsServices.approvals(),
            new InMemoryIncidentRepository(),
            () -> "INC-TEST-001",
            new SlaPolicy(),
            fixedClock
        );
    }

    private OpsAlert alert(String alertId) {
        return new OpsAlert(
            alertId,
            "payment-service",
            "prod",
            "P1",
            "支付服务错误率升高",
            "支付回调接口出现大量 timeout，P95 延迟明显升高。",
            Instant.parse("2026-06-11T06:59:00Z")
        );
    }
}
