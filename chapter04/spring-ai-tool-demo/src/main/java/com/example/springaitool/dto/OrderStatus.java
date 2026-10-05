package com.example.springaitool.dto;

import java.math.BigDecimal;

public record OrderStatus(
    String orderId,
    String customerId,
    String status,
    BigDecimal amount,
    String nextAction
) {
}
