package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.SupportTicket;
import com.example.aicustomerservice.entity.TicketPriority;
import com.example.aicustomerservice.entity.TicketStatus;
import com.example.aicustomerservice.entity.TicketTimelineEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * PostgreSQL 工单DAO。
 *
 * 课程里使用 JDBC 是为了把表结构和对象映射讲清楚。
 * 真实项目也可以替换成 MyBatis、jOOQ 或 JPA。
 */
public class PostgreSqlTicketDao implements TicketDao {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public PostgreSqlTicketDao(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public SupportTicket save(SupportTicket ticket) {
        transactionTemplate.executeWithoutResult(status -> {
            upsertTicket(ticket);
            rewriteTimeline(ticket);
        });
        return ticket;
    }

    @Override
    public Optional<SupportTicket> findByTicketId(String ticketId) {
        return findOne("SELECT * FROM cs_ticket WHERE ticket_id = ?", ticketId);
    }

    @Override
    public Optional<SupportTicket> findByIdempotencyKey(String idempotencyKey) {
        return findOne("SELECT * FROM cs_ticket WHERE idempotency_key = ?", idempotencyKey);
    }

    @Override
    public List<SupportTicket> search(TicketQuery query) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM cs_ticket WHERE 1 = 1");

        if (query.status() != null) {
            sql.append(" AND status = ?");
            params.add(query.status().name());
        }
        if (query.customerId() != null && !query.customerId().isBlank()) {
            sql.append(" AND customer_id = ?");
            params.add(query.customerId());
        }

        sql.append(" ORDER BY created_at DESC LIMIT 100");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapTicketRow(rs), params.toArray())
            .stream()
            .map(row -> row.toTicket(findTimeline(row.ticketId())))
            .toList();
    }

    private Optional<SupportTicket> findOne(String sql, Object param) {
        List<TicketRow> rows = jdbcTemplate.query(sql, (rs, rowNum) -> mapTicketRow(rs), param);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        TicketRow row = rows.get(0);
        return Optional.of(row.toTicket(findTimeline(row.ticketId())));
    }

    private void upsertTicket(SupportTicket ticket) {
        jdbcTemplate.update(
            """
                INSERT INTO cs_ticket (
                    ticket_id, customer_id, session_id, question, answer_draft, intent,
                    status, priority, owner, idempotency_key, created_at, updated_at,
                    resolved_at, duplicate_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (ticket_id) DO UPDATE SET
                    customer_id = EXCLUDED.customer_id,
                    session_id = EXCLUDED.session_id,
                    question = EXCLUDED.question,
                    answer_draft = EXCLUDED.answer_draft,
                    intent = EXCLUDED.intent,
                    status = EXCLUDED.status,
                    priority = EXCLUDED.priority,
                    owner = EXCLUDED.owner,
                    idempotency_key = EXCLUDED.idempotency_key,
                    updated_at = EXCLUDED.updated_at,
                    resolved_at = EXCLUDED.resolved_at,
                    duplicate_count = EXCLUDED.duplicate_count
                """,
            ticket.ticketId(),
            ticket.customerId(),
            ticket.sessionId(),
            ticket.question(),
            ticket.answerDraft(),
            ticket.intent(),
            ticket.status().name(),
            ticket.priority().name(),
            ticket.owner(),
            ticket.idempotencyKey(),
            timestamp(ticket.createdAt()),
            timestamp(ticket.updatedAt()),
            timestamp(ticket.resolvedAt()),
            ticket.duplicateCount()
        );
    }

    private void rewriteTimeline(SupportTicket ticket) {
        jdbcTemplate.update("DELETE FROM cs_ticket_timeline WHERE ticket_id = ?", ticket.ticketId());
        for (int i = 0; i < ticket.timeline().size(); i++) {
            TicketTimelineEvent event = ticket.timeline().get(i);
            jdbcTemplate.update(
                """
                    INSERT INTO cs_ticket_timeline (
                        ticket_id, sequence_no, occurred_at, event_type, event_operator, message
                    ) VALUES (?, ?, ?, ?, ?, ?)
                    """,
                ticket.ticketId(),
                i,
                timestamp(event.occurredAt()),
                event.type(),
                event.operator(),
                event.message()
            );
        }
    }

    private List<TicketTimelineEvent> findTimeline(String ticketId) {
        return jdbcTemplate.query(
            """
                SELECT occurred_at, event_type, event_operator, message
                FROM cs_ticket_timeline
                WHERE ticket_id = ?
                ORDER BY sequence_no ASC
                """,
            (rs, rowNum) -> new TicketTimelineEvent(
                instant(rs, "occurred_at"),
                rs.getString("event_type"),
                rs.getString("event_operator"),
                rs.getString("message")
            ),
            ticketId
        );
    }

    private TicketRow mapTicketRow(ResultSet rs) throws SQLException {
        return new TicketRow(
            rs.getString("ticket_id"),
            rs.getString("customer_id"),
            rs.getString("session_id"),
            rs.getString("question"),
            rs.getString("answer_draft"),
            rs.getString("intent"),
            TicketStatus.valueOf(rs.getString("status")),
            TicketPriority.valueOf(rs.getString("priority")),
            rs.getString("owner"),
            rs.getString("idempotency_key"),
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

    private record TicketRow(
        String ticketId,
        String customerId,
        String sessionId,
        String question,
        String answerDraft,
        String intent,
        TicketStatus status,
        TicketPriority priority,
        String owner,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        int duplicateCount
    ) {
        private SupportTicket toTicket(List<TicketTimelineEvent> timeline) {
            return new SupportTicket(
                ticketId,
                customerId,
                sessionId,
                question,
                answerDraft,
                intent,
                status,
                priority,
                owner,
                idempotencyKey,
                createdAt,
                updatedAt,
                resolvedAt,
                duplicateCount,
                timeline
            );
        }
    }
}
