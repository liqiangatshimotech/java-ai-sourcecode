package com.example.aicustomerservice.entity;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerOrder(
    String orderNo,
    String customerId,
    String customerName,
    OrderStatus status,
    BigDecimal amount,
    String productSummary,
    String channel,
    String paymentTime,
    String receiverMasked,
    String riskLevel,
    Instant createdAt,
    Instant updatedAt
) {
    public OrderSnapshot toSnapshot() {
        return new OrderSnapshot(orderNo, customerId, status.name(), amount, productSummary, paymentTime);
    }

    public CustomerOrder updateStatus(OrderStatus newStatus, Instant now) {
        return new CustomerOrder(
            orderNo,
            customerId,
            customerName,
            newStatus,
            amount,
            productSummary,
            channel,
            paymentTime,
            receiverMasked,
            riskLevel,
            createdAt,
            now
        );
    }
}
