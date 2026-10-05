package com.example.aicustomerservice.entity;

import java.math.BigDecimal;

public record OrderSnapshot(
    String orderNo,
    String customerId,
    String status,
    BigDecimal amount,
    String productSummary,
    String paymentTime
) {
}
