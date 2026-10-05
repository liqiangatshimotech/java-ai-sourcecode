package com.example.langgraph4j.aftersales.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** 退款执行成功后的回执。 */
public record RefundReceipt(String transactionId, String orderId, BigDecimal amount, Instant createdAt) {
}
