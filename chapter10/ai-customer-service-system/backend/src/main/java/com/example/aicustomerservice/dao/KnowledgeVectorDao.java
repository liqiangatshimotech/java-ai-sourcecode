package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.KnowledgeChunk;
import com.example.aicustomerservice.entity.KnowledgeDocument;
import com.example.aicustomerservice.entity.RetrievedKnowledge;
import java.util.List;

public interface KnowledgeVectorDao {

    String embeddingModelName();

    int embeddingDimensions();

    void syncDocument(KnowledgeDocument document, List<KnowledgeChunk> chunks);

    void deleteDocument(String documentId);

    List<RetrievedKnowledge> search(String query, int limit);
}
