package com.example.mcpprotocol.domain;

public record Order(
    String orderId,
    String tenantId,
    String customerName,
    String status,
    int amountCents,
    String internalNote
) {
}
