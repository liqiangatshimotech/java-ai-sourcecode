package com.example.langchain4jmcpserver.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ToolAuditLog {

    private final List<Entry> entries = new ArrayList<>();

    public void record(String userId, String toolName, String businessKey, String result) {
        entries.add(new Entry(Instant.now(), userId, toolName, businessKey, result));
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public record Entry(Instant at, String userId, String toolName, String businessKey, String result) {
    }
}
