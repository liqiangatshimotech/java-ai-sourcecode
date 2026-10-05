package com.example.langchain4jtool.dto;

import java.math.BigDecimal;

public record OrderStatus(
    String orderId,
    String customerId,
    String status,
    BigDecimal amount,
    String nextAction
) {
}
