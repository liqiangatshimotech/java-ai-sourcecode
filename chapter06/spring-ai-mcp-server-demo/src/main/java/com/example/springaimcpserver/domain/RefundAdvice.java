package com.example.springaimcpserver.domain;

public record RefundAdvice(
    String orderId,
    String decision,
    String reason,
    boolean supervisorApprovalRequired
) {
}
