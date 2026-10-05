package com.example.agentscopeframework.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.Map;

/**
 * 给 Agent 使用的业务工具。
 *
 * <p>课程里先用内存数据模拟真实系统。换到生产环境时，这些方法通常会改成调用 CRM、订单系统、物流系统、风控系统或知识库。
 */
public class CustomerTicketTools {

    private final Map<String, String> orders =
            Map.of(
                    "ORDER-1001", "订单状态：已付款，仓库已拣货，预计 24 小时内出库。",
                    "ORDER-2002", "订单状态：支付成功但库存不足，系统已创建补货任务。",
                    "ORDER-3003", "订单状态：已签收，物流回执显示本人签收。");

    private final Map<String, String> policies =
            Map.of(
                    "refund", "退款政策：未发货订单可自动退款；已发货订单需要先完成退货入库。",
                    "shipping", "物流政策：普通用户 48 小时内发货，会员用户 24 小时内优先发货。",
                    "invoice", "发票政策：电子发票通常在订单完成后 10 分钟内生成。");

    @Tool(name = "query_order_status", description = "Query order status by order id")
    public String queryOrderStatus(
            @ToolParam(name = "order_id", description = "Order id, for example ORDER-1001")
                    String orderId) {
        return orders.getOrDefault(orderId, "未找到订单：" + orderId);
    }

    @Tool(name = "search_policy", description = "Search customer service policy by topic")
    public String searchPolicy(
            @ToolParam(name = "topic", description = "Policy topic, such as refund or shipping")
                    String topic) {
        return policies.getOrDefault(topic.toLowerCase(), "未找到政策主题：" + topic);
    }

    @Tool(name = "create_escalation", description = "Create an escalation task for human review")
    public String createEscalation(
            @ToolParam(name = "ticket_id", description = "Customer ticket id") String ticketId,
            @ToolParam(name = "reason", description = "Why this ticket needs human review")
                    String reason) {
        return "已创建人工复核任务：ticketId=%s, reason=%s".formatted(ticketId, reason);
    }
}
