package com.example.langchain4jmcpserver.domain;

import java.util.Map;
import java.util.Optional;

public final class OrderRepository {

    private final Map<String, OrderRecord> orders = Map.of(
        "ORD-1001", new OrderRecord("ORD-1001", "acme", "林一", "PAID", 129900, 2, false, "首单用户，禁止直接承诺补偿"),
        "ORD-1002", new OrderRecord("ORD-1002", "acme", "周明", "SHIPPED", 26900, 9, true, "用户上周已申请过一次补差"),
        "ORD-2001", new OrderRecord("ORD-2001", "beta", "王宁", "PAID", 39900, 1, false, "其他租户订单，不能泄露")
    );

    public Optional<OrderRecord> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }
}
