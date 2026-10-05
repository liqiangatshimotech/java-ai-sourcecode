package com.example.springaimcpserver.domain;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class OrderRepository {

    private final Map<String, OrderRecord> orders = Map.of(
        "ORD-1001", new OrderRecord(
            "ORD-1001",
            "acme",
            "张三",
            "PAID",
            128_00,
            false,
            "内部备注：该客户曾走过人工补偿流程，不能返回给模型。"
        ),
        "ORD-1002", new OrderRecord(
            "ORD-1002",
            "acme",
            "王五",
            "SHIPPED",
            699_00,
            true,
            "内部备注：物流异常已升级。"
        ),
        "ORD-2001", new OrderRecord(
            "ORD-2001",
            "beta",
            "李四",
            "REFUNDING",
            399_00,
            false,
            "内部备注：Beta 租户订单，ACME 用户不能读取。"
        )
    );

    public Optional<OrderRecord> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }
}
