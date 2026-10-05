package com.example.langgraph4j.aftersales.service;

import com.example.langgraph4j.aftersales.domain.OrderSnapshot;
import com.example.langgraph4j.aftersales.domain.RefundReceipt;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等退款实现。相同工作流线程即使因超时重试，也只会得到同一张退款回执。
 */
@Service
public class IdempotentRefundExecutionService implements RefundExecutionService {

    private final Map<String, RefundReceipt> receipts = new ConcurrentHashMap<>();

    @Override
    public RefundReceipt refund(String idempotencyKey, OrderSnapshot order) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("退款幂等键不能为空");
        }
        return receipts.computeIfAbsent(idempotencyKey, key -> new RefundReceipt(
                "RF-" + UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)),
                order.orderId(), order.paidAmount(), Instant.now()));
    }

    /** 只供测试和运行观测使用。 */
    public int executedCount() {
        return receipts.size();
    }
}
