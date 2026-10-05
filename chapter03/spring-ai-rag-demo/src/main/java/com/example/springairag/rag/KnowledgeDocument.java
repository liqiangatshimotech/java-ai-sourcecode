package com.example.springairag.rag;

import java.util.List;
import java.util.Map;

public record KnowledgeDocument(
    String id,
    String title,
    String tenantId,
    String source,
    List<String> tags,
    String content,
    Map<String, String> metadata
) {
}
