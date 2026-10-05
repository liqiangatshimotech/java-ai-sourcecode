package com.example.enterpriseopsagent.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 一次可追踪的排障事件。
 *
 * 它不是简单保存模型回答，而是把告警、报告、审批单、SLA 和时间线放到同一个业务对象里。
 */
public record DiagnosisIncident(
    String incidentId,
    OpsAlert alert,
    DiagnosisReport report,
    IncidentStatus status,
    String approvalTicketId,
    Instant slaDeadline,
    Instant createdAt,
    Instant updatedAt,
    Instant resolvedAt,
    int duplicateCount,
    List<IncidentTimelineEvent> timeline
) {
    public DiagnosisIncident {
        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException("incidentId 不能为空");
        }
        if (alert == null) {
            throw new IllegalArgumentException("alert 不能为空");
        }
        if (report == null) {
            throw new IllegalArgumentException("report 不能为空");
        }
        if (status == null) {
            throw new IllegalArgumentException("status 不能为空");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (timeline == null) {
            timeline = List.of();
        } else {
            timeline = List.copyOf(timeline);
        }
    }

    public boolean slaBreached(Instant now) {
        if (slaDeadline == null || status.isClosed()) {
            return false;
        }
        return now.isAfter(slaDeadline);
    }

    public DiagnosisIncident markDuplicate(Instant now) {
        List<IncidentTimelineEvent> nextTimeline = appendEvent(
            new IncidentTimelineEvent(now, "DUPLICATE_ALERT_RECEIVED", "alert-platform", "收到重复告警，返回已有排障事件。")
        );
        return new DiagnosisIncident(
            incidentId,
            alert,
            report,
            status,
            approvalTicketId,
            slaDeadline,
            createdAt,
            now,
            resolvedAt,
            duplicateCount + 1,
            nextTimeline
        );
    }

    public DiagnosisIncident applyDecision(DecisionType decision, String operator, String comment, Instant now) {
        if (status != IncidentStatus.WAITING_APPROVAL) {
            throw new IllegalStateException("只有 WAITING_APPROVAL 状态可以审批");
        }

        IncidentStatus nextStatus = decision == DecisionType.APPROVE
            ? IncidentStatus.APPROVED
            : IncidentStatus.REJECTED;
        String message = decision == DecisionType.APPROVE
            ? "审批通过：" + comment
            : "审批拒绝：" + comment;

        return new DiagnosisIncident(
            incidentId,
            alert,
            report,
            nextStatus,
            approvalTicketId,
            slaDeadline,
            createdAt,
            now,
            resolvedAt,
            duplicateCount,
            appendEvent(new IncidentTimelineEvent(now, "APPROVAL_" + decision.name(), operator, message))
        );
    }

    public DiagnosisIncident resolve(String operator, String summary, Instant now) {
        if (status == IncidentStatus.REJECTED) {
            throw new IllegalStateException("审批被拒绝的事件不能直接关闭");
        }
        if (status == IncidentStatus.RESOLVED) {
            return this;
        }

        return new DiagnosisIncident(
            incidentId,
            alert,
            report,
            IncidentStatus.RESOLVED,
            approvalTicketId,
            slaDeadline,
            createdAt,
            now,
            now,
            duplicateCount,
            appendEvent(new IncidentTimelineEvent(now, "INCIDENT_RESOLVED", operator, summary))
        );
    }

    private List<IncidentTimelineEvent> appendEvent(IncidentTimelineEvent event) {
        List<IncidentTimelineEvent> nextTimeline = new ArrayList<>(timeline);
        nextTimeline.add(event);
        return nextTimeline;
    }
}
