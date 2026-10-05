package com.example.langchain4jtool.dto;

import java.time.Instant;
import java.util.Map;

public record ToolAuditEntry(
    Instant occurredAt,
    String toolName,
    Map<String, String> arguments,
    String resultSummary
) {
}
