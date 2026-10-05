package com.example.mcpprotocol.domain;

import java.util.Map;
import java.util.Optional;

public final class OrderRepository {

    private final Map<String, Order> orders = Map.of(
        "ORD-1001", new Order(
            "ORD-1001",
            "acme",
            "张三",
            "PAID",
            128_00,
            "内部备注：该客户曾走过人工补偿流程，不能直接暴露给模型。"
        ),
        "ORD-2001", new Order(
            "ORD-2001",
            "beta",
            "李四",
            "REFUNDING",
            399_00,
            "内部备注：Beta 租户数据，acme 用户无权读取。"
        )
    );

    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }
}
