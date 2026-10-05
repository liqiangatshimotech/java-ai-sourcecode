package com.example.existingservicemcpclient.existing;

public record OrderStatusSnapshot(
    String orderId,
    String status,
    boolean refundable,
    String summary
) {
}
