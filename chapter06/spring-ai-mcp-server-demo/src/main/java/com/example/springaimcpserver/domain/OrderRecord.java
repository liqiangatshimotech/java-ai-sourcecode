package com.example.springaimcpserver.domain;

public record OrderRecord(
    String orderId,
    String tenantId,
    String customerName,
    String status,
    int amountCents,
    boolean shipped,
    String internalNote
) {
}
