package com.example.aicustomerservice.service;

import com.example.aicustomerservice.common.BusinessException;
import com.example.aicustomerservice.dao.OrderDao;
import com.example.aicustomerservice.dao.OrderQuery;
import com.example.aicustomerservice.entity.CustomerOrder;
import com.example.aicustomerservice.entity.OrderStatus;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OrderService {

    /**
     * 订单状态迁移表：key 是当前状态，value 是允许进入的下一个状态。
     *
     * 与 frontend/js/order-detail.js 的 allowedOrderTransitions 保持一致。页面只展示合法按钮，
     * 但真正的校验必须在 Java 里做，否则绕过页面直接调接口就能把已关闭的订单改回拣货中。
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
        OrderStatus.PAID_PICKING, Set.of(OrderStatus.SHIPPED, OrderStatus.REFUNDING, OrderStatus.CLOSED),
        OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED, OrderStatus.REFUNDING),
        OrderStatus.DELIVERED, Set.of(OrderStatus.REFUNDING, OrderStatus.CLOSED),
        OrderStatus.REFUNDING, Set.of(OrderStatus.CLOSED),
        OrderStatus.CLOSED, Set.of()
    );

    private final OrderDao orderDao;
    private final Clock clock;

    public OrderService(OrderDao orderDao, Clock clock) {
        this.orderDao = orderDao;
        this.clock = clock;
    }

    public List<CustomerOrder> search(OrderStatus status, String customerId, String keyword) {
        return orderDao.search(new OrderQuery(status, customerId, keyword));
    }

    public CustomerOrder get(String orderNo) {
        return orderDao.findByOrderNo(orderNo)
            .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "订单不存在：" + orderNo));
    }

    public CustomerOrder updateStatus(String orderNo, OrderStatus status) {
        CustomerOrder order = get(orderNo);
        // 保存前校验状态迁移：不在迁移表里的变更（包括目标状态等于当前状态）一律拒绝。
        if (!ALLOWED_TRANSITIONS.getOrDefault(order.status(), Set.of()).contains(status)) {
            throw new BusinessException(
                "ORDER_STATUS_TRANSITION_INVALID",
                "订单 " + orderNo + " 不能从 " + order.status() + " 变更为 " + status
            );
        }
        return orderDao.save(order.updateStatus(status, clock.instant()));
    }
}
