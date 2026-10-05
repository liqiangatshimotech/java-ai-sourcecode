package com.example.langchain4jmcpserver.domain;

public record OrderStatus(
    String orderId,
    String customerName,
    String status,
    double amountYuan,
    String nextStep
) {
}
