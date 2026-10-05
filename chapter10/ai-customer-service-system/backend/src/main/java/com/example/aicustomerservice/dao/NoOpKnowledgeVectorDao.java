package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.KnowledgeChunk;
import com.example.aicustomerservice.entity.KnowledgeDocument;
import com.example.aicustomerservice.entity.RetrievedKnowledge;
import java.util.List;

public class NoOpKnowledgeVectorDao implements KnowledgeVectorDao {

    @Override
    public String embeddingModelName() {
        return "";
    }

    @Override
    public int embeddingDimensions() {
        return 0;
    }

    @Override
    public void syncDocument(KnowledgeDocument document, List<KnowledgeChunk> chunks) {
    }

    @Override
    public void deleteDocument(String documentId) {
    }

    @Override
    public List<RetrievedKnowledge> search(String query, int limit) {
        return List.of();
    }
}
