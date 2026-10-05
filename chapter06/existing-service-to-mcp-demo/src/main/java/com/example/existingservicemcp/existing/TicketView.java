package com.example.existingservicemcp.existing;

public record TicketView(
    String ticketId,
    String customerName,
    CustomerTier customerTier,
    String channel,
    String subject,
    TicketStatus status,
    String publicSummary
) {
}
