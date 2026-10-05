package com.example.springaimcpserver.domain;

public record OrderStatus(
    String orderId,
    String customerName,
    String status,
    double amountYuan,
    boolean shipped,
    boolean refundable,
    String summary
) {
}
