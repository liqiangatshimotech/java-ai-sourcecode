package com.example.aicustomerservice.entity;

public record SupportIntent(
    IntentType type,
    double confidence,
    String reason
) {
}
