package com.example.aicustomerservice.service;

import com.example.aicustomerservice.dao.OrderDao;
import com.example.aicustomerservice.entity.LogisticsSnapshot;
import com.example.aicustomerservice.entity.OrderSnapshot;
import com.example.aicustomerservice.entity.RefundAssessment;
import java.util.Map;
import java.util.Optional;

/**
 * 外部业务系统模拟。
 *
 * 在生产系统里，订单、物流、退款可能来自不同微服务。AI 客服不应该直接访问数据库，
 * 而是通过这里定义的工具接口拿到受权限控制的业务结果。
 */
public class InMemoryCustomerToolService implements CustomerToolService {

    private final OrderDao orderDao;

    private final Map<String, LogisticsSnapshot> logistics = Map.of(
        "SO202606210002", new LogisticsSnapshot("SO202606210002", "顺丰", "SF10000002", "IN_TRANSIT", "包裹已到达杭州转运中心"),
        "SO202606210003", new LogisticsSnapshot("SO202606210003", "中通", "ZT10000003", "DELIVERED", "已由本人签收")
    );

    public InMemoryCustomerToolService(OrderDao orderDao) {
        this.orderDao = orderDao;
    }

    @Override
    public Optional<OrderSnapshot> findOrder(String customerId, String orderNo) {
        return orderDao.findByOrderNo(orderNo)
            .filter(order -> order.customerId().equals(customerId))
            .map(order -> order.toSnapshot());
    }

    @Override
    public Optional<LogisticsSnapshot> findLogistics(String orderNo) {
        return Optional.ofNullable(logistics.get(orderNo));
    }

    @Override
    public RefundAssessment assessRefund(String customerId, String orderNo) {
        return findOrder(customerId, orderNo)
            .map(order -> {
                if (isNonReturnableProduct(order.productSummary())) {
                    return new RefundAssessment(orderNo, false, "商品为日用品，根据退货规则不支持退货。", "无法办理自动退货。");
                }
                return switch (order.status()) {
                    case "DELIVERED" -> new RefundAssessment(orderNo, true, "订单已签收，符合 7 天无理由退款基础条件。", "可以引导用户提交退款申请。");
                    case "PAID_PICKING" -> new RefundAssessment(orderNo, false, "订单已进入仓库拣货流程，自动退款可能导致仓配状态不一致。", "创建售后工单，由人工确认是否拦截发货。");
                    case "SHIPPED" -> new RefundAssessment(orderNo, false, "订单已发货，需要确认物流是否可拦截。", "先创建物流拦截工单，再根据拦截结果处理退款。");
                    default -> new RefundAssessment(orderNo, false, "当前订单状态暂不支持自动退款。", "创建工单给售后团队处理。");
                };
            })
            .orElseGet(() -> new RefundAssessment(orderNo, false, "没有查到该客户名下订单。", "创建工单并核对订单号。"));
    }

    private boolean isNonReturnableProduct(String productSummary) {
        return productSummary != null && productSummary.contains("日用品");
    }
}
