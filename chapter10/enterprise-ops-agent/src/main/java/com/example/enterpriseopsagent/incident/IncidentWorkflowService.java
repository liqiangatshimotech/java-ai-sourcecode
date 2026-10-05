package com.example.enterpriseopsagent.incident;

import com.example.enterpriseopsagent.agent.OpsAgentService;
import com.example.enterpriseopsagent.domain.DecisionType;
import com.example.enterpriseopsagent.domain.DiagnosisIncident;
import com.example.enterpriseopsagent.domain.DiagnosisReport;
import com.example.enterpriseopsagent.domain.IncidentStatus;
import com.example.enterpriseopsagent.domain.IncidentTimelineEvent;
import com.example.enterpriseopsagent.domain.OpsAlert;
import com.example.enterpriseopsagent.tool.ApprovalService;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 排障事件工作流服务。
 *
 * 这一层把“告警接入、Agent 诊断、审批创建、状态推进、查询回放”串起来，
 * 对外表现为一个真实企业系统，而不是一次模型调用。
 */
public class IncidentWorkflowService {

    private final OpsAgentService opsAgentService;
    private final ApprovalService approvalService;
    private final IncidentRepository incidentRepository;
    private final IncidentIdGenerator incidentIdGenerator;
    private final SlaPolicy slaPolicy;
    private final Clock clock;

    public IncidentWorkflowService(
        OpsAgentService opsAgentService,
        ApprovalService approvalService,
        IncidentRepository incidentRepository,
        IncidentIdGenerator incidentIdGenerator,
        SlaPolicy slaPolicy,
        Clock clock
    ) {
        this.opsAgentService = opsAgentService;
        this.approvalService = approvalService;
        this.incidentRepository = incidentRepository;
        this.incidentIdGenerator = incidentIdGenerator;
        this.slaPolicy = slaPolicy;
        this.clock = clock;
    }

    public DiagnosisIncident ingestAlert(OpsAlert alert) {
        Instant now = clock.instant();
        return incidentRepository.findByAlertId(alert.alertId())
            .map(existingIncident -> incidentRepository.save(existingIncident.markDuplicate(now)))
            .orElseGet(() -> createNewIncident(alert, now));
    }

    public DiagnosisIncident getIncident(String incidentId) {
        return incidentRepository.findByIncidentId(incidentId)
            .orElseThrow(() -> new IncidentNotFoundException(incidentId));
    }

    public List<DiagnosisIncident> search(IncidentQuery query) {
        return incidentRepository.search(query);
    }

    public DiagnosisIncident decide(String incidentId, DecisionType decision, String operator, String comment) {
        DiagnosisIncident incident = getIncident(incidentId);
        DiagnosisIncident updatedIncident = incident.applyDecision(decision, operator, comment, clock.instant());
        return incidentRepository.save(updatedIncident);
    }

    public DiagnosisIncident resolve(String incidentId, String operator, String summary) {
        DiagnosisIncident incident = getIncident(incidentId);
        DiagnosisIncident updatedIncident = incident.resolve(operator, summary, clock.instant());
        return incidentRepository.save(updatedIncident);
    }

    private DiagnosisIncident createNewIncident(OpsAlert alert, Instant now) {
        DiagnosisReport report = opsAgentService.diagnose(alert);
        String approvalTicketId = null;
        IncidentStatus status = report.requiresApproval()
            ? IncidentStatus.WAITING_APPROVAL
            : IncidentStatus.DIAGNOSED;

        List<IncidentTimelineEvent> timeline = new ArrayList<>();
        timeline.add(new IncidentTimelineEvent(now, "ALERT_RECEIVED", "alert-platform", "收到告警：" + alert.title()));
        timeline.add(new IncidentTimelineEvent(now, "DIAGNOSIS_GENERATED", "ops-agent", "Agent 已生成排障报告。"));

        if (report.requiresApproval()) {
            approvalTicketId = approvalService.createRollbackApproval(
                alert.serviceName(),
                alert.environment(),
                report.recommendation()
            );
            timeline.add(new IncidentTimelineEvent(
                now,
                "APPROVAL_REQUESTED",
                "ops-agent",
                "已创建审批单：" + approvalTicketId
            ));
        }

        DiagnosisIncident incident = new DiagnosisIncident(
            incidentIdGenerator.nextIncidentId(),
            alert,
            report,
            status,
            approvalTicketId,
            slaPolicy.responseDeadline(alert, now),
            now,
            now,
            null,
            0,
            timeline
        );
        return incidentRepository.save(incident);
    }
}
