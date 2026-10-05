package com.example.enterpriseopsagent.incident;

import com.example.enterpriseopsagent.domain.DiagnosisIncident;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存版排障事件仓储。
 *
 * 它模拟真实项目里的 incident 表和 alert_id 唯一索引，方便不接数据库也能跑通完整业务流程。
 */
public class InMemoryIncidentRepository implements IncidentRepository {

    private final ConcurrentMap<String, DiagnosisIncident> incidentsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> incidentIdByAlertId = new ConcurrentHashMap<>();

    @Override
    public DiagnosisIncident save(DiagnosisIncident incident) {
        incidentsById.put(incident.incidentId(), incident);
        incidentIdByAlertId.put(incident.alert().alertId(), incident.incidentId());
        return incident;
    }

    @Override
    public Optional<DiagnosisIncident> findByIncidentId(String incidentId) {
        return Optional.ofNullable(incidentsById.get(incidentId));
    }

    @Override
    public Optional<DiagnosisIncident> findByAlertId(String alertId) {
        String incidentId = incidentIdByAlertId.get(alertId);
        if (incidentId == null) {
            return Optional.empty();
        }
        return findByIncidentId(incidentId);
    }

    @Override
    public List<DiagnosisIncident> search(IncidentQuery query) {
        return incidentsById.values().stream()
            .filter(incident -> query.status() == null || incident.status() == query.status())
            .filter(incident -> isBlank(query.serviceName()) || incident.alert().serviceName().equals(query.serviceName()))
            .filter(incident -> isBlank(query.environment()) || incident.alert().environment().equals(query.environment()))
            .sorted(Comparator.comparing(DiagnosisIncident::createdAt).reversed())
            .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
