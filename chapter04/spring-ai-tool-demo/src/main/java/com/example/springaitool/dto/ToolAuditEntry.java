package com.example.springaitool.dto;

import java.time.Instant;
import java.util.Map;

public record ToolAuditEntry(
    Instant occurredAt,
    String toolName,
    Map<String, String> arguments,
    String resultSummary
) {
}
