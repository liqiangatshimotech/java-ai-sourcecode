package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.AiRunAudit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;

/** PostgreSQL AI Run DAO，列表摘要使用 JSONB 保存，便于后续增加更多审计字段。 */
public class PostgreSqlAiRunDao implements AiRunDao {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PostgreSqlAiRunDao(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiRunAudit save(AiRunAudit run) {
        jdbcTemplate.update(
            """
                INSERT INTO cs_ai_run (
                    request_id, conversation_id, provider, model, intent, confidence,
                    source_summaries, tool_summaries, guardrail_summaries,
                    elapsed_ms, handoff, created_at, status, error_code, prompt_version, input_tokens, output_tokens
                ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (request_id) DO UPDATE SET
                    conversation_id = EXCLUDED.conversation_id,
                    provider = EXCLUDED.provider,
                    model = EXCLUDED.model,
                    intent = EXCLUDED.intent,
                    confidence = EXCLUDED.confidence,
                    source_summaries = EXCLUDED.source_summaries,
                    tool_summaries = EXCLUDED.tool_summaries,
                    guardrail_summaries = EXCLUDED.guardrail_summaries,
                    elapsed_ms = EXCLUDED.elapsed_ms,
                    handoff = EXCLUDED.handoff,
                    created_at = EXCLUDED.created_at,
                    status = EXCLUDED.status, error_code = EXCLUDED.error_code,
                    prompt_version = EXCLUDED.prompt_version,
                    input_tokens = EXCLUDED.input_tokens, output_tokens = EXCLUDED.output_tokens
                """,
            run.requestId(), run.conversationId(), run.provider(), run.model(), run.intent(), run.confidence(),
            json(run.sourceSummaries()), json(run.toolSummaries()), json(run.guardrailSummaries()),
            run.elapsedMs(), run.handoff(), Timestamp.from(run.createdAt()), run.status(), run.errorCode(),
            run.promptVersion(), run.inputTokens(), run.outputTokens()
        );
        return run;
    }

    @Override
    public Optional<AiRunAudit> findByRequestId(String requestId) {
        List<AiRunAudit> rows = jdbcTemplate.query(
            "SELECT * FROM cs_ai_run WHERE request_id = ?",
            (rs, rowNum) -> mapRun(rs),
            requestId
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<AiRunAudit> search(String conversationId, Boolean handoff, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM cs_ai_run WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        if (conversationId != null && !conversationId.isBlank()) {
            sql.append(" AND conversation_id = ?");
            parameters.add(conversationId.trim());
        }
        if (handoff != null) {
            sql.append(" AND handoff = ?");
            parameters.add(handoff);
        }
        sql.append(" ORDER BY created_at DESC LIMIT ?");
        parameters.add(Math.max(1, Math.min(limit, 200)));
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapRun(rs), parameters.toArray());
    }

    private AiRunAudit mapRun(ResultSet rs) throws SQLException {
        return new AiRunAudit(
            rs.getString("request_id"),
            rs.getString("conversation_id"),
            rs.getString("provider"),
            rs.getString("model"),
            rs.getString("intent"),
            rs.getDouble("confidence"),
            strings(rs.getString("source_summaries")),
            strings(rs.getString("tool_summaries")),
            strings(rs.getString("guardrail_summaries")),
            rs.getLong("elapsed_ms"),
            rs.getBoolean("handoff"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getString("status"), rs.getString("error_code"), rs.getString("prompt_version"),
            rs.getObject("input_tokens", Long.class), rs.getObject("output_tokens", Long.class)
        );
    }

    private String json(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("AI Run 摘要无法序列化", exception);
        }
    }

    private List<String> strings(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI Run 摘要无法反序列化", exception);
        }
    }
}
