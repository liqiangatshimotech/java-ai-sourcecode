package com.example.enterpriseopsagent.web;

import com.example.enterpriseopsagent.domain.DiagnosisIncident;
import com.example.enterpriseopsagent.domain.IncidentStatus;

import java.time.Instant;

public record IncidentSummaryResponse(
    String incidentId,
    String alertId,
    String serviceName,
    String environment,
    String severity,
    IncidentStatus status,
    boolean slaBreached,
    Instant slaDeadline,
    Instant updatedAt
) {
    public static IncidentSummaryResponse from(DiagnosisIncident incident, Instant now) {
        return new IncidentSummaryResponse(
            incident.incidentId(),
            incident.alert().alertId(),
            incident.alert().serviceName(),
            incident.alert().environment(),
            incident.alert().severity(),
            incident.status(),
            incident.slaBreached(now),
            incident.slaDeadline(),
            incident.updatedAt()
        );
    }
}
