package com.example.langgraph4j.aftersales.service;

import com.example.langgraph4j.aftersales.domain.OrderSnapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 教学用订单仓库。三张订单分别覆盖人工审批、自动退款和高风险拒绝路径。
 */
@Service
public class InMemoryOrderQueryService implements OrderQueryService {

    private final Map<String, OrderSnapshot> orders = Map.of(
            "ORD-1001", new OrderSnapshot("ORD-1001", "Java AI 实战课", new BigDecimal("129.00"),
                    LocalDate.now().minusDays(3), false, false),
            "ORD-1002", new OrderSnapshot("ORD-1002", "AI 提示词手册", new BigDecimal("49.00"),
                    LocalDate.now().minusDays(2), false, false),
            "ORD-9009", new OrderSnapshot("ORD-9009", "GPU 计算服务", new BigDecimal("899.00"),
                    LocalDate.now().minusDays(1), false, true));

    @Override
    public OrderSnapshot findRequired(String orderId) {
        OrderSnapshot order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在：" + orderId);
        }
        return order;
    }
}
