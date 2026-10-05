package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.OrderStatus;

public record OrderQuery(
    OrderStatus status,
    String customerId,
    String keyword
) {
}
