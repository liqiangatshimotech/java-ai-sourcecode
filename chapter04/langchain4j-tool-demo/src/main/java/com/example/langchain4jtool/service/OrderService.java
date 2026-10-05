package com.example.langchain4jtool.service;

import com.example.langchain4jtool.dto.OrderStatus;

import java.math.BigDecimal;
import java.util.Map;

public class OrderService {

    private final Map<String, OrderStatus> orders = Map.of(
        "O-1001", new OrderStatus("O-1001", "C-2001", "SHIPPED", new BigDecimal("899.00"), "告知客户快递已发出"),
        "O-1002", new OrderStatus("O-1002", "C-2002", "PAYMENT_PENDING", new BigDecimal("1299.00"), "提醒客户完成支付"),
        "O-1003", new OrderStatus("O-1003", "C-2003", "REFUND_REVIEW", new BigDecimal("499.00"), "转人工复核退款材料")
    );

    public OrderStatus findByOrderId(String orderId) {
        String normalized = requireId(orderId, "orderId");
        OrderStatus order = this.orders.get(normalized);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在: " + normalized);
        }
        return order;
    }

    private static String requireId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return value.trim().toUpperCase();
    }
}
