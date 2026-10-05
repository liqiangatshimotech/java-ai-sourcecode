package com.example.aicustomerservice.entity;

public record CustomerProfile(
    String customerId,
    String displayName,
    CustomerTier tier,
    String phoneMasked
) {
}
