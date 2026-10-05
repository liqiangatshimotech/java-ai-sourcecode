package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.CustomerOrder;
import com.example.aicustomerservice.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderDao implements OrderDao {

    private final Map<String, CustomerOrder> orders = new ConcurrentHashMap<>();

    public InMemoryOrderDao(Clock clock) {
        Instant now = clock.instant();
        save(new CustomerOrder(
            "SO202606210001",
            "C10001",
            "北京朝阳门店",
            OrderStatus.PAID_PICKING,
            new BigDecimal("1299.00"),
            "企业门店补货套装 x 3",
            "B2B_CONTRACT",
            "2026-06-21 09:42:10",
            "北京市朝阳区 ***",
            "HIGH_VALUE",
            now.minusSeconds(7200),
            now.minusSeconds(1800)
        ));
        save(new CustomerOrder(
            "SO202606210002",
            "C10002",
            "华东 VIP 客户",
            OrderStatus.SHIPPED,
            new BigDecimal("299.00"),
            "会员精选礼盒 x 1",
            "APP",
            "2026-06-20 18:20:00",
            "杭州市西湖区 ***",
            "NORMAL",
            now.minusSeconds(86400),
            now.minusSeconds(3600)
        ));
        save(new CustomerOrder(
            "SO202606210003",
            "C10003",
            "普通注册用户",
            OrderStatus.DELIVERED,
            new BigDecimal("89.00"),
            "日用品组合 x 1",
            "MINI_PROGRAM",
            "2026-06-18 12:00:00",
            "上海市浦东新区 ***",
            "LOW",
            now.minusSeconds(259200),
            now.minusSeconds(172800)
        ));
        save(new CustomerOrder(
            "SO202606210004",
            "C10001",
            "北京朝阳门店",
            OrderStatus.REFUNDING,
            new BigDecimal("5600.00"),
            "企业大客户活动物料 x 20",
            "B2B_CONTRACT",
            "2026-06-19 10:10:00",
            "北京市朝阳区 ***",
            "FINANCE_REVIEW",
            now.minusSeconds(172800),
            now.minusSeconds(5400)
        ));
    }

    @Override
    public List<CustomerOrder> search(OrderQuery query) {
        String keyword = query.keyword() == null ? "" : query.keyword().trim().toLowerCase();
        return orders.values()
            .stream()
            .filter(order -> query.status() == null || order.status() == query.status())
            .filter(order -> query.customerId() == null || query.customerId().isBlank() || order.customerId().equals(query.customerId()))
            .filter(order -> keyword.isBlank()
                || order.orderNo().toLowerCase().contains(keyword)
                || order.customerName().toLowerCase().contains(keyword)
                || order.productSummary().toLowerCase().contains(keyword))
            .sorted(Comparator.comparing(CustomerOrder::updatedAt).reversed())
            .toList();
    }

    @Override
    public Optional<CustomerOrder> findByOrderNo(String orderNo) {
        return Optional.ofNullable(orders.get(orderNo));
    }

    @Override
    public CustomerOrder save(CustomerOrder order) {
        orders.put(order.orderNo(), order);
        return order;
    }
}
