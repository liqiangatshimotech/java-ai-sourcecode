package com.example.enterpriseopsagent.incident;

import com.example.enterpriseopsagent.domain.DiagnosisIncident;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository {

    DiagnosisIncident save(DiagnosisIncident incident);

    Optional<DiagnosisIncident> findByIncidentId(String incidentId);

    Optional<DiagnosisIncident> findByAlertId(String alertId);

    List<DiagnosisIncident> search(IncidentQuery query);
}
