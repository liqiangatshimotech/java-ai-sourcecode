package com.example.enterpriseopsagent.incident;

import com.example.enterpriseopsagent.domain.DiagnosisIncident;
import com.example.enterpriseopsagent.domain.DiagnosisReport;
import com.example.enterpriseopsagent.domain.IncidentStatus;
import com.example.enterpriseopsagent.domain.IncidentTimelineEvent;
import com.example.enterpriseopsagent.domain.OpsAlert;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MySQL 版 incident 仓储。
 *
 * 这里故意使用 Spring JDBC，而不是 JPA，方便直接看到业务对象和表字段如何对应。
 */
public class MySqlIncidentRepository implements IncidentRepository {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public MySqlIncidentRepository(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public DiagnosisIncident save(DiagnosisIncident incident) {
        transactionTemplate.executeWithoutResult(status -> {
            upsertIncident(incident);
            rewriteTimeline(incident);
        });
        return incident;
    }

    @Override
    public Optional<DiagnosisIncident> findByIncidentId(String incidentId) {
        return findOne("SELECT * FROM ops_incident WHERE incident_id = ?", incidentId);
    }

    @Override
    public Optional<DiagnosisIncident> findByAlertId(String alertId) {
        return findOne("SELECT * FROM ops_incident WHERE alert_id = ?", alertId);
    }

    @Override
    public List<DiagnosisIncident> search(IncidentQuery query) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM ops_incident WHERE 1 = 1");

        if (query.status() != null) {
            sql.append(" AND status = ?");
            params.add(query.status().name());
        }
        if (!isBlank(query.serviceName())) {
            sql.append(" AND service_name = ?");
            params.add(query.serviceName());
        }
        if (!isBlank(query.environment())) {
            sql.append(" AND environment = ?");
            params.add(query.environment());
        }

        sql.append(" ORDER BY created_at DESC LIMIT 100");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapIncidentRow(rs), params.toArray())
            .stream()
            .map(row -> row.toIncident(findTimeline(row.incidentId())))
            .toList();
    }

    private Optional<DiagnosisIncident> findOne(String sql, Object param) {
        List<IncidentRow> rows = jdbcTemplate.query(sql, (rs, rowNum) -> mapIncidentRow(rs), param);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        IncidentRow row = rows.get(0);
        return Optional.of(row.toIncident(findTimeline(row.incidentId())));
    }

    private void upsertIncident(DiagnosisIncident incident) {
        jdbcTemplate.update(
            """
                INSERT INTO ops_incident (
                    incident_id, alert_id, service_name, environment, severity, title, description, triggered_at,
                    report_summary, root_cause_hypothesis, evidence, recommendation, requires_approval,
                    status, approval_ticket_id, sla_deadline, created_at, updated_at, resolved_at, duplicate_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    alert_id = VALUES(alert_id),
                    service_name = VALUES(service_name),
                    environment = VALUES(environment),
                    severity = VALUES(severity),
                    title = VALUES(title),
                    description = VALUES(description),
                    triggered_at = VALUES(triggered_at),
                    report_summary = VALUES(report_summary),
                    root_cause_hypothesis = VALUES(root_cause_hypothesis),
                    evidence = VALUES(evidence),
                    recommendation = VALUES(recommendation),
                    requires_approval = VALUES(requires_approval),
                    status = VALUES(status),
                    approval_ticket_id = VALUES(approval_ticket_id),
                    sla_deadline = VALUES(sla_deadline),
                    created_at = VALUES(created_at),
                    updated_at = VALUES(updated_at),
                    resolved_at = VALUES(resolved_at),
                    duplicate_count = VALUES(duplicate_count)
                """,
            incident.incidentId(),
            incident.alert().alertId(),
            incident.alert().serviceName(),
            incident.alert().environment(),
            incident.alert().severity(),
            incident.alert().title(),
            incident.alert().description(),
            timestamp(incident.alert().triggeredAt()),
            incident.report().summary(),
            incident.report().rootCauseHypothesis(),
            incident.report().evidence(),
            incident.report().recommendation(),
            incident.report().requiresApproval(),
            incident.status().name(),
            incident.approvalTicketId(),
            timestamp(incident.slaDeadline()),
            timestamp(incident.createdAt()),
            timestamp(incident.updatedAt()),
            timestamp(incident.resolvedAt()),
            incident.duplicateCount()
        );
    }

    private void rewriteTimeline(DiagnosisIncident incident) {
        jdbcTemplate.update("DELETE FROM ops_incident_timeline WHERE incident_id = ?", incident.incidentId());
        for (int i = 0; i < incident.timeline().size(); i++) {
            IncidentTimelineEvent event = incident.timeline().get(i);
            jdbcTemplate.update(
                """
                    INSERT INTO ops_incident_timeline (
                        incident_id, sequence_no, occurred_at, event_type, event_operator, message
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """,
                incident.incidentId(),
                i,
                timestamp(event.occurredAt()),
                event.type(),
                event.operator(),
                event.message()
            );
        }
    }

    private List<IncidentTimelineEvent> findTimeline(String incidentId) {
        return jdbcTemplate.query(
            """
                SELECT occurred_at, event_type, event_operator, message
                FROM ops_incident_timeline
                WHERE incident_id = ?
                ORDER BY sequence_no ASC
                """,
            (rs, rowNum) -> new IncidentTimelineEvent(
                instant(rs, "occurred_at"),
                rs.getString("event_type"),
                rs.getString("event_operator"),
                rs.getString("message")
            ),
            incidentId
        );
    }

    private IncidentRow mapIncidentRow(ResultSet rs) throws SQLException {
        return new IncidentRow(
            rs.getString("incident_id"),
            rs.getString("alert_id"),
            rs.getString("service_name"),
            rs.getString("environment"),
            rs.getString("severity"),
            rs.getString("title"),
            rs.getString("description"),
            instant(rs, "triggered_at"),
            rs.getString("report_summary"),
            rs.getString("root_cause_hypothesis"),
            rs.getString("evidence"),
            rs.getString("recommendation"),
            rs.getBoolean("requires_approval"),
            IncidentStatus.valueOf(rs.getString("status")),
            rs.getString("approval_ticket_id"),
            instant(rs, "sla_deadline"),
            instant(rs, "created_at"),
            instant(rs, "updated_at"),
            instant(rs, "resolved_at"),
            rs.getInt("duplicate_count")
        );
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private Instant instant(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record IncidentRow(
        String incidentId,
        String alertId,
        String serviceName,
        String environment,
        String severity,
        String title,
        String description,
        Instant triggeredAt,
        String reportSummary,
        String rootCauseHypothesis,
        String evidence,
        String recommendation,
        boolean requiresApproval,
        IncidentStatus status,
        String approvalTicketId,
        Instant slaDeadline,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        int duplicateCount
    ) {
        private DiagnosisIncident toIncident(List<IncidentTimelineEvent> timeline) {
            OpsAlert alert = new OpsAlert(
                alertId,
                serviceName,
                environment,
                severity,
                title,
                description,
                triggeredAt
            );
            DiagnosisReport report = new DiagnosisReport(
                alertId,
                reportSummary,
                rootCauseHypothesis,
                evidence,
                recommendation,
                requiresApproval
            );
            return new DiagnosisIncident(
                incidentId,
                alert,
                report,
                status,
                approvalTicketId,
                slaDeadline,
                createdAt,
                updatedAt,
                resolvedAt,
                duplicateCount,
                timeline
            );
        }
    }
}
