package com.example.aicustomerservice.entity;

public record RefundAssessment(
    String orderNo,
    boolean canAutoRefund,
    String reason,
    String nextStep
) {
}
