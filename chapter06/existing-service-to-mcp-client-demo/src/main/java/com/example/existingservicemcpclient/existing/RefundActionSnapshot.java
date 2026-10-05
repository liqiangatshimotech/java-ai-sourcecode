package com.example.existingservicemcpclient.existing;

public record RefundActionSnapshot(
    String orderId,
    String decision,
    boolean supervisorApprovalRequired,
    String reason
) {
}
