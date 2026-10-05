package com.example.langgraph4j.aftersales.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 图中只使用的订单快照，避免节点直接依赖数据库实体。 */
public record OrderSnapshot(
        String orderId,
        String productName,
        BigDecimal paidAmount,
        LocalDate deliveredDate,
        boolean alreadyRefunded,
        boolean suspiciousAccount) {
}
