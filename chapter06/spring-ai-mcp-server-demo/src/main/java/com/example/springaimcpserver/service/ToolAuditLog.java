package com.example.springaimcpserver.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class ToolAuditLog {

    private final List<String> entries = new ArrayList<>();

    public void record(String userId, String toolName, String target, String result) {
        entries.add("%s user=%s tool=%s target=%s result=%s".formatted(
            Instant.now(), userId, toolName, target, result
        ));
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }
}
