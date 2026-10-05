package com.example.langgraph4j.aftersales.service;

import com.example.langgraph4j.aftersales.domain.OrderSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdempotentRefundExecutionServiceTest {

    @Test
    void sameIdempotencyKeyReturnsSameReceipt() {
        IdempotentRefundExecutionService service = new IdempotentRefundExecutionService();
        OrderSnapshot order = new OrderSnapshot("ORD-1", "test", new BigDecimal("10"),
                LocalDate.now(), false, false);

        var first = service.refund("same-thread", order);
        var second = service.refund("same-thread", order);

        assertEquals(first.transactionId(), second.transactionId());
        assertEquals(1, service.executedCount());
    }
}
