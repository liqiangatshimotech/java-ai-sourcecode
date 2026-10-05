package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.KnowledgeChunk;
import com.example.aicustomerservice.entity.KnowledgeDocument;
import com.example.aicustomerservice.entity.RetrievedKnowledge;
import com.example.aicustomerservice.service.EmbeddingService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PostgreSQL + pgvector 向量检索。
 *
 * 每个知识 chunk 存在 cs_knowledge_chunk.embedding，小数据集采用精确距离排序；确定模型维度后可自行创建对应维度的 HNSW 索引。
 */
public class PgVectorKnowledgeVectorDao implements KnowledgeVectorDao {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;

    public PgVectorKnowledgeVectorDao(JdbcTemplate jdbcTemplate, EmbeddingService embeddingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
    }

    @Override
    public String embeddingModelName() {
        return embeddingService.modelName();
    }

    @Override
    public int embeddingDimensions() {
        return embeddingService.dimensions();
    }

    @Override
    public void syncDocument(KnowledgeDocument document, List<KnowledgeChunk> chunks) {
        if (!document.published()) {
            deleteDocument(document.documentId());
            return;
        }
        for (KnowledgeChunk chunk : chunks) {
            jdbcTemplate.update(
                """
                    UPDATE cs_knowledge_chunk
                    SET status = ?,
                        embedding = CAST(? AS vector),
                        embedding_status = 'SYNCED',
                        embedding_model = ?,
                        embedding_dim = ?,
                        updated_at = ?
                    WHERE chunk_id = ?
                    """,
                document.status().name(),
                vectorLiteral(embeddingService.embed(chunk.title() + "\n" + chunk.text())),
                embeddingService.modelName(),
                embeddingService.dimensions(),
                java.sql.Timestamp.from(document.updatedAt()),
                chunk.chunkId()
            );
        }
    }

    @Override
    public void deleteDocument(String documentId) {
        jdbcTemplate.update(
            """
                UPDATE cs_knowledge_chunk
                SET status = 'DISABLED',
                    embedding_status = 'DELETED',
                    updated_at = now()
                WHERE document_id = ?
                """,
            documentId
        );
    }

    @Override
    public List<RetrievedKnowledge> search(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }
        String queryVector = vectorLiteral(embeddingService.embed(query));
        return jdbcTemplate.query(
            """
                SELECT chunk_id,
                       document_id,
                       version,
                       title,
                       chunk_text,
                       1 - (embedding <=> CAST(? AS vector)) AS score
                FROM cs_knowledge_chunk
                WHERE status = 'PUBLISHED'
                    AND embedding_status = 'SYNCED'
                    AND embedding_model = ? AND embedding_dim = ?
                    AND EXISTS (SELECT 1 FROM cs_knowledge_document d
                        WHERE d.document_id = cs_knowledge_chunk.document_id AND d.status = 'PUBLISHED')
                    AND embedding IS NOT NULL
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """,
            (rs, rowNum) -> new RetrievedKnowledge(
                rs.getString("chunk_id"),
                rs.getString("document_id"),
                rs.getString("version"),
                rs.getString("title"),
                rs.getString("chunk_text"),
                clampScore(rs.getDouble("score"))
            ),
            queryVector,
            embeddingService.modelName(), embeddingService.dimensions(),
            queryVector,
            Math.max(1, limit)
        );
    }

    private String vectorLiteral(float[] vector) {
        return "[" + java.util.Arrays.stream(toDoubleArray(vector))
            .mapToObj(Double::toString)
            .collect(Collectors.joining(",")) + "]";
    }

    private double[] toDoubleArray(float[] values) {
        double[] vector = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            vector[i] = values[i];
        }
        return vector;
    }

    private double clampScore(double score) {
        return Math.max(0, Math.min(1, score));
    }
}
