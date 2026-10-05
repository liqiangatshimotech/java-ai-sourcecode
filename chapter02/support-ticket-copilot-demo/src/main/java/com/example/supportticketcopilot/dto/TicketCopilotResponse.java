package com.example.supportticketcopilot.dto;

public record TicketCopilotResponse(
    String requestId,
    TicketAnalysis analysis,
    int attempts,
    boolean fallback
) {
}
