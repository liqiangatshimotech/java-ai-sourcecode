package com.example.agentruntime.business;

import com.example.agentruntime.tool.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/** 业务规则由 Java 控制；创建工单不是发放补偿，两种结果不可混为一谈。 */
public final class AfterSalesTools {
    private final OrderRepository orderRepository;
    private final LogisticsGateway logisticsGateway;
    private final Map<String, String> tickets = new ConcurrentHashMap<>();
    public AfterSalesTools(OrderRepository orderRepository, LogisticsGateway logisticsGateway) {
        this.orderRepository = orderRepository; this.logisticsGateway = logisticsGateway;
    }
    public ToolObservation queryOrder(ToolCall call) {
        OrderSnapshot order = orderRepository.findByOrderNo(call.arguments().get("orderNo"));
        return ToolObservation.success("订单 " + order.orderNo() + " 已发货",
                Map.of("orderStatus", order.status()));
    }
    public ToolObservation queryLogistics(ToolCall call) {
        LogisticsSnapshot logistics = logisticsGateway.query(call.arguments().get("orderNo"));
        return ToolObservation.success("物流已停滞 " + logistics.hoursSinceLastUpdate() + " 小时",
                Map.of("logisticsDelayHours", String.valueOf(logistics.hoursSinceLastUpdate())));
    }
    public ToolObservation evaluateCompensation(ToolCall call) {
        // 重新查可信业务数据，不接受 Planner 自报金额或资格。
        OrderSnapshot order = orderRepository.findByOrderNo(call.arguments().get("orderNo"));
        LogisticsSnapshot logistics = logisticsGateway.query(order.orderNo());
        boolean eligible = order.shippedHoursAgo() > order.promiseHours()
                && logistics.hoursSinceLastUpdate() >= 12;
        String reason = eligible
                ? "订单超过承诺送达时间且物流停滞至少 12 小时，符合补偿候选条件"
                : "暂未达到补偿条件，继续观察物流";
        return ToolObservation.success(reason, Map.of(
                "compensationDecision", eligible ? "ELIGIBLE" : "WAIT",
                "compensationReason", reason,
                "compensationAmount", eligible ? "20" : "0"));
    }
    public ToolObservation draftCustomerReply(ToolCall call) {
        ToolObservation evaluation = evaluateCompensation(call);
        String reply = "ELIGIBLE".equals(evaluation.facts().get("compensationDecision"))
                ? "订单 " + call.arguments().get("orderNo") + " 已达到补偿候选条件，创建工单需要人工确认；目前尚未发放补偿。"
                : "已核实物流情况，目前暂不符合补偿规则。";
        return ToolObservation.success(reply, Map.of("customerReply", reply));
    }
    public ToolObservation createCompensationTicket(ToolCall call) {
        // 审批后业务事实可能已变化，执行前再次校验资格。
        if (!"ELIGIBLE".equals(evaluateCompensation(call).facts().get("compensationDecision"))) {
            throw new IllegalStateException("当前订单不符合补偿条件，不能创建工单");
        }
        String orderNo = call.arguments().get("orderNo");
        // 本例每单仅一个补偿工单；生产应把该约束持久化到业务库。
        String ticketNo = tickets.computeIfAbsent(orderNo, key -> "TICKET-" + key);
        return ToolObservation.success("已创建补偿工单 " + ticketNo + "，尚未发放补偿",
                Map.of("ticketNo", ticketNo));
    }
}
