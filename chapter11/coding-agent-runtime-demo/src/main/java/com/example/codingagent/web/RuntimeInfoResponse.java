package com.example.codingagent.web;

import java.util.List;

public record RuntimeInfoResponse(
        String provider,
        String model,
        String baseUrl,
        boolean apiKeyConfigured,
        List<String> tools,
        String workspaceBase
) {
}
