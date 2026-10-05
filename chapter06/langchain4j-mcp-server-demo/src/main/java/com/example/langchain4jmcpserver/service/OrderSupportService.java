package com.example.langchain4jmcpserver.service;

import com.example.langchain4jmcpserver.domain.OrderRecord;
import com.example.langchain4jmcpserver.domain.OrderRepository;
import com.example.langchain4jmcpserver.domain.OrderStatus;
import com.example.langchain4jmcpserver.domain.RefundAdvice;
import com.example.langchain4jmcpserver.security.CurrentUser;

public final class OrderSupportService {

    private final OrderRepository orderRepository;

    public OrderSupportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderStatus queryOrderStatus(CurrentUser user, String orderId) {
        OrderRecord order = findVisibleOrder(user, orderId);
        String nextStep = order.shipped()
            ? "物流已发出，先核对签收和售后时效。"
            : "订单已支付未发货，可以优先核对库存和发货计划。";

        return new OrderStatus(
            order.orderId(),
            order.customerName(),
            order.status(),
            order.amountCents() / 100.0,
            nextStep
        );
    }

    public RefundAdvice suggestRefundAction(CurrentUser user, String orderId) {
        OrderRecord order = findVisibleOrder(user, orderId);
        if (order.shipped()) {
            return new RefundAdvice(
                order.orderId(),
                "MANUAL_REVIEW",
                "订单已发货，退款前需要先确认签收、退货和售后时效。",
                0,
                true
            );
        }

        boolean withinSevenDays = order.paidDaysAgo() <= 7;
        return new RefundAdvice(
            order.orderId(),
            withinSevenDays ? "ALLOW_REFUND" : "MANUAL_REVIEW",
            withinSevenDays ? "订单未发货且支付未超过 7 天，可以按原路退款。" : "支付时间较久，需要人工复核。",
            withinSevenDays ? order.amountCents() / 100.0 : 0,
            !withinSevenDays
        );
    }

    private OrderRecord findVisibleOrder(CurrentUser user, String orderId) {
        if (!user.hasRole("support_agent")) {
            throw new SecurityException("当前用户缺少 support_agent 角色，不能调用售后工具。");
        }

        OrderRecord order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在：" + orderId));
        if (!order.tenantId().equals(user.tenantId())) {
            throw new SecurityException("当前用户无权读取该租户订单。");
        }
        return order;
    }
}
