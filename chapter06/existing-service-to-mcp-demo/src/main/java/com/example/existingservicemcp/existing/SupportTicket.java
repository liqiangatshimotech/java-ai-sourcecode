package com.example.existingservicemcp.existing;

public record SupportTicket(
    String ticketId,
    String tenantId,
    String customerName,
    CustomerTier customerTier,
    String channel,
    String subject,
    String content,
    TicketStatus status,
    String internalNote
) {
}
