package com.example.aicustomerservice.entity;

public record LogisticsSnapshot(
    String orderNo,
    String carrier,
    String trackingNo,
    String status,
    String latestEvent
) {
}
