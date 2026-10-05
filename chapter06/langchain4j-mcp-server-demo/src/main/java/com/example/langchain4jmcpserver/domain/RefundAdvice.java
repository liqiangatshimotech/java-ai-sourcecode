package com.example.langchain4jmcpserver.domain;

public record RefundAdvice(
    String orderId,
    String decision,
    String reason,
    double maxRefundYuan,
    boolean manualReviewRequired
) {
}
