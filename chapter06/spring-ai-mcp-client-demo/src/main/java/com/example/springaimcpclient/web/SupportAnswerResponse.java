package com.example.springaimcpclient.web;

import java.util.List;

public record SupportAnswerResponse(
    String provider,
    String question,
    String answer,
    List<McpToolView> availableTools
) {
}
