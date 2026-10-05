package com.example.langchain4jmcpserver.domain;

public record OrderRecord(
    String orderId,
    String tenantId,
    String customerName,
    String status,
    int amountCents,
    int paidDaysAgo,
    boolean shipped,
    String internalNote
) {
}
