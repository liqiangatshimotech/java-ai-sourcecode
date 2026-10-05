package com.example.langchain4jtool.service;

import com.example.langchain4jtool.dto.ToolAuditEntry;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToolAuditLog {

    private final Clock clock;
    private final List<ToolAuditEntry> entries = new ArrayList<>();

    public ToolAuditLog() {
        this(Clock.systemUTC());
    }

    ToolAuditLog(Clock clock) {
        this.clock = clock;
    }

    public void record(String toolName, Map<String, String> arguments, String resultSummary) {
        this.entries.add(new ToolAuditEntry(
            Instant.now(this.clock),
            toolName,
            Map.copyOf(arguments),
            resultSummary
        ));
    }

    public List<ToolAuditEntry> entries() {
        return List.copyOf(this.entries);
    }
}
