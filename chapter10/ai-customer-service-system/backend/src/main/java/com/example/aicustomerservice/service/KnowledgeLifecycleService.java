package com.example.aicustomerservice.service;

import com.example.aicustomerservice.common.BusinessException;
import com.example.aicustomerservice.common.Hashes;
import com.example.aicustomerservice.dao.KnowledgeDao;
import com.example.aicustomerservice.dao.KnowledgeVectorDao;
import com.example.aicustomerservice.dao.NoOpKnowledgeVectorDao;
import com.example.aicustomerservice.entity.KnowledgeChunk;
import com.example.aicustomerservice.entity.KnowledgeDocument;
import com.example.aicustomerservice.entity.KnowledgeStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * 知识导入和发布流程。
 *
 * 真实企业里一般会把导入、审核、发布拆成多个系统任务。
 * 这里把流程集中在一个服务中，方便看清知识从“原始文本”进入 RAG 的完整路径。
 */
public class KnowledgeLifecycleService {

    private final KnowledgeDao knowledgeDao;
    private final SemanticChunker chunker;
    private final Clock clock;
    private final KnowledgeVectorDao vectorDao;

    public KnowledgeLifecycleService(KnowledgeDao knowledgeDao, SemanticChunker chunker, Clock clock) {
        this(knowledgeDao, chunker, clock, new NoOpKnowledgeVectorDao());
    }

    public KnowledgeLifecycleService(
        KnowledgeDao knowledgeDao,
        SemanticChunker chunker,
        Clock clock,
        KnowledgeVectorDao vectorDao
    ) {
        this.knowledgeDao = knowledgeDao;
        this.chunker = chunker;
        this.clock = clock;
        this.vectorDao = vectorDao;
    }

    public KnowledgeDocument importDraft(
        String title,
        String content,
        String version,
        String sourceType,
        String sourceName,
        String operator
    ) {
        Instant now = clock.instant();
        String normalizedVersion = blankToDefault(version, "draft-" + Hashes.sha256Short(title + now));
        String documentId = "KB-" + Hashes.sha256Short(title + content + normalizedVersion).toUpperCase();
        KnowledgeDocument document = new KnowledgeDocument(
            documentId,
            normalizedVersion,
            title.trim(),
            content.trim(),
            KnowledgeStatus.DRAFT,
            blankToDefault(sourceType, "MANUAL"),
            blankToDefault(sourceName, "console"),
            blankToDefault(operator, "online-operator"),
            now,
            now
        );
        return knowledgeDao.save(document);
    }

    public List<KnowledgeDocument> search(String keyword, KnowledgeStatus status) {
        return knowledgeDao.search(keyword, status);
    }

    public KnowledgeDocument get(String documentId) {
        return knowledgeDao.findByDocumentId(documentId)
            .orElseThrow(() -> new BusinessException("KNOWLEDGE_NOT_FOUND", "知识文档不存在：" + documentId));
    }

    public List<KnowledgeChunk> previewChunks(String documentId) {
        return chunker.split(get(documentId));
    }

    public KnowledgeDocument submitReview(String documentId, String operator) {
        KnowledgeDocument document = get(documentId);
        if (document.status() != KnowledgeStatus.DRAFT) {
            throw new BusinessException("KNOWLEDGE_STATUS_INVALID", "只有草稿知识可以提交审核：" + documentId);
        }
        return knowledgeDao.save(document.withStatus(
            KnowledgeStatus.REVIEWING,
            blankToDefault(operator, "online-operator"),
            clock.instant()
        ));
    }

    public synchronized KnowledgeDocument publish(String documentId, String operator) {
        KnowledgeDocument document = get(documentId);
        if (document.status() != KnowledgeStatus.REVIEWING && document.status() != KnowledgeStatus.DRAFT) {
            throw new BusinessException("KNOWLEDGE_STATUS_INVALID", "只有草稿或审核中的知识可以发布：" + documentId);
        }
        List<KnowledgeChunk> chunks = chunker.split(document);
        if (chunks.isEmpty()) {
            throw new BusinessException("KNOWLEDGE_EMPTY_CHUNK", "知识内容无法切分出有效段落：" + documentId);
        }
        KnowledgeDocument published = document.withStatus(
            KnowledgeStatus.PUBLISHED,
            blankToDefault(operator, "online-operator"),
            clock.instant()
        );
        // 先生成完整索引，最后切换文档发布态。失败保留 DRAFT/REVIEWING，可重试同一 publish 请求。
        // 向量查询同时过滤主文档状态，因此部分同步的 chunk 不能提前进入答案。
        knowledgeDao.replaceChunks(published, chunks);
        vectorDao.syncDocument(published, chunks);
        knowledgeDao.markChunksSynced(
            published.documentId(),
            vectorDao.embeddingModelName(),
            vectorDao.embeddingDimensions()
        );
        return knowledgeDao.save(published);
    }

    public KnowledgeDocument disable(String documentId, String operator) {
        KnowledgeDocument document = get(documentId);
        // 即使已下线仍重试清理向量，修复上次外部同步失败后无法重试的问题。
        KnowledgeDocument disabled = knowledgeDao.save(document.withStatus(
            KnowledgeStatus.DISABLED,
            blankToDefault(operator, "online-operator"),
            clock.instant()
        ));
        vectorDao.deleteDocument(disabled.documentId());
        knowledgeDao.markDocumentChunksDeleted(disabled.documentId());
        return disabled;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
