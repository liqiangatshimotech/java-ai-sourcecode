package com.example.springaimcpserver.service;

import com.example.springaimcpserver.domain.OrderRecord;
import com.example.springaimcpserver.domain.OrderRepository;
import com.example.springaimcpserver.domain.OrderStatus;
import com.example.springaimcpserver.domain.RefundAdvice;
import com.example.springaimcpserver.security.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderStatus queryOrderStatus(CurrentUser user, String orderId) {
        OrderRecord order = loadAllowedOrder(user, orderId);
        boolean refundable = "PAID".equals(order.status()) && !order.shipped();
        return new OrderStatus(
            order.orderId(),
            order.customerName(),
            order.status(),
            order.amountCents() / 100.0,
            order.shipped(),
            refundable,
            "订单 %s 当前状态为 %s，金额 %.2f 元。".formatted(
                order.orderId(), order.status(), order.amountCents() / 100.0
            )
        );
    }

    public RefundAdvice suggestRefundAction(CurrentUser user, String orderId) {
        OrderRecord order = loadAllowedOrder(user, orderId);
        boolean supervisorApprovalRequired = order.amountCents() > 500_00;

        if ("PAID".equals(order.status()) && !order.shipped()) {
            return new RefundAdvice(
                order.orderId(),
                supervisorApprovalRequired ? "ESCALATE" : "APPROVE",
                supervisorApprovalRequired ? "订单金额超过 500 元，需要主管审批。" : "订单已支付且未发货，符合直接退款条件。",
                supervisorApprovalRequired
            );
        }

        if (order.shipped()) {
            return new RefundAdvice(
                order.orderId(),
                "ASK_RETURN_FIRST",
                "订单已发货，应先引导客户完成退货入库，再进入退款流程。",
                false
            );
        }

        return new RefundAdvice(order.orderId(), "MANUAL_REVIEW", "订单状态需要人工复核。", true);
    }

    private OrderRecord loadAllowedOrder(CurrentUser user, String orderId) {
        if (!user.hasRole("support_agent")) {
            throw new SecurityException("当前用户缺少 support_agent 角色。");
        }

        OrderRecord order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("订单不存在：" + orderId));

        if (!order.tenantId().equals(user.tenantId())) {
            throw new SecurityException("当前用户无权读取该租户订单。");
        }
        return order;
    }
}
