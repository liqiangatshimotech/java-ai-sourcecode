package com.example.agentruntime.business;

import java.util.Map;
/** A20260001 对应文章；A100/A200 保留早期示例订单，均为本地虚构数据。 */
public final class DemoOrderData implements OrderRepository, LogisticsGateway {
    private final Map<String, OrderSnapshot> orders = Map.of(
            "A20260001", new OrderSnapshot("A20260001", "SHIPPED", 72, 48),
            "A100", new OrderSnapshot("A100", "SHIPPED", 72, 48),
            "A200", new OrderSnapshot("A200", "SHIPPED", 24, 48));
    public OrderSnapshot findByOrderNo(String orderNo) {
        OrderSnapshot order = orders.get(orderNo);
        if (order == null) throw new IllegalArgumentException("订单不存在：" + orderNo);
        return order;
    }
    public LogisticsSnapshot query(String orderNo) {
        findByOrderNo(orderNo); // 未知订单不能生成看似真实的物流事实。
        return new LogisticsSnapshot(orderNo, "A200".equals(orderNo) ? 2 : 18);
    }
}
