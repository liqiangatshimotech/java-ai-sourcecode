package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.KnowledgeChunk;
import com.example.aicustomerservice.entity.KnowledgeDocument;
import com.example.aicustomerservice.entity.KnowledgeStatus;
import java.util.List;
import java.util.Optional;

public interface KnowledgeDao {

    List<KnowledgeDocument> findPublishedDocuments();

    List<KnowledgeDocument> search(String keyword, KnowledgeStatus status);

    Optional<KnowledgeDocument> findByDocumentId(String documentId);

    KnowledgeDocument save(KnowledgeDocument document);

    default void replaceChunks(KnowledgeDocument document, List<KnowledgeChunk> chunks) {
    }

    default void markChunksSynced(String documentId, String embeddingModel, int embeddingDimensions) {
    }

    default void markDocumentChunksDeleted(String documentId) {
    }
}
