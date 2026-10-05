package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.KnowledgeChunk;
import com.example.aicustomerservice.entity.KnowledgeDocument;
import com.example.aicustomerservice.entity.KnowledgeStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PostgreSQL 知识库主存储。
 *
 * 原文、chunk 元数据和 pgvector 向量字段都落在同一个 PostgreSQL 实例。
 */
public class PostgreSqlKnowledgeDao implements KnowledgeDao {

    private final JdbcTemplate jdbcTemplate;

    public PostgreSqlKnowledgeDao(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        seedBuiltInDocuments(clock.instant());
    }

    @Override
    public List<KnowledgeDocument> findPublishedDocuments() {
        return jdbcTemplate.query(
            """
                SELECT *
                FROM cs_knowledge_document
                WHERE status = 'PUBLISHED'
                ORDER BY updated_at DESC
                """,
            (rs, rowNum) -> mapDocument(rs)
        );
    }

    @Override
    public List<KnowledgeDocument> search(String keyword, KnowledgeStatus status) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM cs_knowledge_document WHERE 1 = 1");
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status.name());
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                 AND (
                    document_id ILIKE ?
                    OR title ILIKE ?
                    OR content ILIKE ?
                    OR source_name ILIKE ?
                 )
                """);
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY updated_at DESC LIMIT 100");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapDocument(rs), params.toArray());
    }

    @Override
    public Optional<KnowledgeDocument> findByDocumentId(String documentId) {
        List<KnowledgeDocument> documents = jdbcTemplate.query(
            "SELECT * FROM cs_knowledge_document WHERE document_id = ?",
            (rs, rowNum) -> mapDocument(rs),
            documentId
        );
        return documents.stream().findFirst();
    }

    @Override
    public KnowledgeDocument save(KnowledgeDocument document) {
        jdbcTemplate.update(
            """
                INSERT INTO cs_knowledge_document (
                    document_id, version, title, content, status, source_type, source_name,
                    operator, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (document_id) DO UPDATE SET
                    version = EXCLUDED.version,
                    title = EXCLUDED.title,
                    content = EXCLUDED.content,
                    status = EXCLUDED.status,
                    source_type = EXCLUDED.source_type,
                    source_name = EXCLUDED.source_name,
                    operator = EXCLUDED.operator,
                    updated_at = EXCLUDED.updated_at
                """,
            document.documentId(),
            document.version(),
            document.title(),
            document.content(),
            document.status().name(),
            document.sourceType(),
            document.sourceName(),
            document.operator(),
            timestamp(document.createdAt()),
            timestamp(document.updatedAt())
        );
        return document;
    }

    @Override
    public void replaceChunks(KnowledgeDocument document, List<KnowledgeChunk> chunks) {
        jdbcTemplate.update("DELETE FROM cs_knowledge_chunk WHERE document_id = ?", document.documentId());
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = chunks.get(i);
            jdbcTemplate.update(
                """
                    INSERT INTO cs_knowledge_chunk (
                        chunk_id, document_id, version, status, chunk_index, title, chunk_text, content_hash,
                        embedding_status, embedding_model, embedding_dim, embedding, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, md5(?), 'PENDING', '', NULL, NULL, ?, ?)
                    """,
                chunk.chunkId(),
                chunk.documentId(),
                chunk.version(),
                document.status().name(),
                i,
                chunk.title(),
                chunk.text(),
                chunk.text(),
                timestamp(document.updatedAt()),
                timestamp(document.updatedAt())
            );
            jdbcTemplate.update(
                """
                    INSERT INTO cs_knowledge_vector_sync (
                        chunk_id, sync_status, last_error, synced_at, updated_at
                    ) VALUES (?, 'PENDING', NULL, NULL, ?)
                    ON CONFLICT (chunk_id) DO UPDATE SET
                        sync_status = EXCLUDED.sync_status,
                        last_error = NULL,
                        synced_at = NULL,
                        updated_at = EXCLUDED.updated_at
                    """,
                chunk.chunkId(),
                timestamp(document.updatedAt())
            );
        }
    }

    @Override
    public void markChunksSynced(String documentId, String embeddingModel, int embeddingDimensions) {
        Instant now = Instant.now();
        jdbcTemplate.update(
            """
                UPDATE cs_knowledge_chunk
                SET embedding_status = 'SYNCED',
                    embedding_model = ?,
                    embedding_dim = ?,
                    updated_at = ?
                WHERE document_id = ?
                """,
            embeddingModel,
            embeddingDimensions,
            timestamp(now),
            documentId
        );
        jdbcTemplate.update(
            """
                UPDATE cs_knowledge_vector_sync sync
                SET sync_status = 'SYNCED',
                    last_error = NULL,
                    synced_at = ?,
                    updated_at = ?
                FROM cs_knowledge_chunk chunk
                WHERE sync.chunk_id = chunk.chunk_id
                    AND chunk.document_id = ?
                """,
            timestamp(now),
            timestamp(now),
            documentId
        );
    }

    @Override
    public void markDocumentChunksDeleted(String documentId) {
        Instant now = Instant.now();
        jdbcTemplate.update(
            """
                UPDATE cs_knowledge_chunk
                SET status = 'DISABLED',
                    embedding_status = 'DELETED',
                    updated_at = ?
                WHERE document_id = ?
                """,
            timestamp(now),
            documentId
        );
        jdbcTemplate.update(
            """
                UPDATE cs_knowledge_vector_sync sync
                SET sync_status = 'DELETED',
                    updated_at = ?
                FROM cs_knowledge_chunk chunk
                WHERE sync.chunk_id = chunk.chunk_id
                    AND chunk.document_id = ?
                """,
            timestamp(now),
            documentId
        );
    }

    private void seedBuiltInDocuments(Instant now) {
        insertIfAbsent(new KnowledgeDocument(
            "KB-REFUND-001",
            "v2026.06",
            "退款与售后规则",
            """
                普通订单在签收后 7 天内可以申请无理由退款，生鲜、定制和虚拟商品除外。
                VIP 客户的退款审核 SLA 为 2 小时，企业客户可以走客户成功专属通道。
                如果订单已经进入仓库拣货完成状态，系统只能先创建售后工单，由人工确认是否拦截发货。
                """,
            KnowledgeStatus.PUBLISHED,
            "BUILT_IN",
            "course-seed",
            "system",
            now,
            now
        ));
        insertIfAbsent(new KnowledgeDocument(
            "KB-SHIPPING-001",
            "v2026.06",
            "物流查询规则",
            """
                物流状态以承运商回传为准。超过 24 小时没有更新时，先查询运单轨迹，再判断是否创建物流催办工单。
                高价值企业订单需要同步客户成功经理，避免只给出模板化回复。
                """,
            KnowledgeStatus.PUBLISHED,
            "BUILT_IN",
            "course-seed",
            "system",
            now,
            now
        ));
        insertIfAbsent(new KnowledgeDocument(
            "KB-INVOICE-001",
            "v2026.06",
            "发票处理规则",
            """
                电子发票通常在支付完成后 10 分钟内生成。抬头错误可以在开票后 24 小时内申请重开。
                企业客户需要专票时，必须校验税号、开户行和注册地址。
                """,
            KnowledgeStatus.PUBLISHED,
            "BUILT_IN",
            "course-seed",
            "system",
            now,
            now
        ));
        insertIfAbsent(new KnowledgeDocument(
            "KB-AI-GUARDRAIL-001",
            "v2026.06",
            "客服回复安全边界",
            """
                回复必须优先依据知识库和业务系统查询结果。不确定时不要编造政策、不要承诺赔付金额，
                应该创建工单并说明需要人工确认。
                """,
            KnowledgeStatus.PUBLISHED,
            "BUILT_IN",
            "course-seed",
            "system",
            now,
            now
        ));
    }

    private void insertIfAbsent(KnowledgeDocument document) {
        jdbcTemplate.update(
            """
                INSERT INTO cs_knowledge_document (
                    document_id, version, title, content, status, source_type, source_name,
                    operator, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (document_id) DO NOTHING
                """,
            document.documentId(),
            document.version(),
            document.title(),
            document.content(),
            document.status().name(),
            document.sourceType(),
            document.sourceName(),
            document.operator(),
            timestamp(document.createdAt()),
            timestamp(document.updatedAt())
        );
    }

    private KnowledgeDocument mapDocument(ResultSet rs) throws SQLException {
        return new KnowledgeDocument(
            rs.getString("document_id"),
            rs.getString("version"),
            rs.getString("title"),
            rs.getString("content"),
            KnowledgeStatus.valueOf(rs.getString("status")),
            rs.getString("source_type"),
            rs.getString("source_name"),
            rs.getString("operator"),
            instant(rs, "created_at"),
            instant(rs, "updated_at")
        );
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private Instant instant(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
