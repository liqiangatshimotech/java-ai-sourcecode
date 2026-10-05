package com.example.existingservicemcp.mcp;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class McpAuditLog {

    private final List<String> entries = new ArrayList<>();

    public void record(String userId, String capability, String target, String result) {
        entries.add("%s user=%s capability=%s target=%s result=%s".formatted(
            Instant.now(), userId, capability, target, result
        ));
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }
}
