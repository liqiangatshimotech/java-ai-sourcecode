package com.example.aicustomerservice.dto;

import java.util.List;

public record KnowledgeSearchResponse(
    String query,
    List<SourceResponse> results
) {
}
