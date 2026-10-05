package com.example.enterpriseopsagent.web;

import com.example.enterpriseopsagent.domain.DiagnosisIncident;
import com.example.enterpriseopsagent.domain.DiagnosisReport;
import com.example.enterpriseopsagent.domain.IncidentStatus;
import com.example.enterpriseopsagent.domain.IncidentTimelineEvent;

import java.time.Instant;
import java.util.List;

public record IncidentResponse(
    String incidentId,
    String alertId,
    String serviceName,
    String environment,
    String severity,
    IncidentStatus status,
    String approvalTicketId,
    Instant slaDeadline,
    boolean slaBreached,
    Instant createdAt,
    Instant updatedAt,
    Instant resolvedAt,
    int duplicateCount,
    DiagnosisReport report,
    List<IncidentTimelineEvent> timeline
) {
    public static IncidentResponse from(DiagnosisIncident incident, Instant now) {
        return new IncidentResponse(
            incident.incidentId(),
            incident.alert().alertId(),
            incident.alert().serviceName(),
            incident.alert().environment(),
            incident.alert().severity(),
            incident.status(),
            incident.approvalTicketId(),
            incident.slaDeadline(),
            incident.slaBreached(now),
            incident.createdAt(),
            incident.updatedAt(),
            incident.resolvedAt(),
            incident.duplicateCount(),
            incident.report(),
            incident.timeline()
        );
    }
}
