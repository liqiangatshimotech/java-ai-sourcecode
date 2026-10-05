package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.CustomerOrder;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
    String orderNo,
    String customerId,
    String customerName,
    String status,
    BigDecimal amount,
    String productSummary,
    String channel,
    String paymentTime,
    String receiverMasked,
    String riskLevel,
    Instant createdAt,
    Instant updatedAt
) {
    public static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(
            order.orderNo(),
            order.customerId(),
            order.customerName(),
            order.status().name(),
            order.amount(),
            order.productSummary(),
            order.channel(),
            order.paymentTime(),
            order.receiverMasked(),
            order.riskLevel(),
            order.createdAt(),
            order.updatedAt()
        );
    }
}
