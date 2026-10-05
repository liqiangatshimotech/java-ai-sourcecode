package com.example.langchain4jmcpserver.tool;

import com.example.langchain4jmcpserver.domain.OrderStatus;
import com.example.langchain4jmcpserver.domain.RefundAdvice;
import com.example.langchain4jmcpserver.security.CurrentUser;
import com.example.langchain4jmcpserver.security.DemoUserContext;
import com.example.langchain4jmcpserver.service.OrderSupportService;
import com.example.langchain4jmcpserver.service.ToolAuditLog;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

public final class SupportMcpTools {

    private final DemoUserContext userContext;
    private final OrderSupportService orderSupportService;
    private final ToolAuditLog auditLog;

    public SupportMcpTools(
        DemoUserContext userContext,
        OrderSupportService orderSupportService,
        ToolAuditLog auditLog
    ) {
        this.userContext = userContext;
        this.orderSupportService = orderSupportService;
        this.auditLog = auditLog;
    }

    @Tool(
        name = "query_order_status",
        value = "按订单号查询当前用户有权访问的订单状态。只返回客服处理需要的字段，不返回内部备注。"
    )
    public OrderStatus queryOrderStatus(
        @P(name = "orderId", value = "订单号，例如 ORD-1001") String orderId
    ) {
        CurrentUser user = userContext.currentUser();
        OrderStatus result = orderSupportService.queryOrderStatus(user, orderId);
        auditLog.record(user.userId(), "query_order_status", orderId, result.status());
        return result;
    }

    @Tool(
        name = "suggest_refund_action",
        value = "根据订单状态和售后规则给出退款处理建议。只返回可执行建议，不直接修改订单。"
    )
    public RefundAdvice suggestRefundAction(
        @P(name = "orderId", value = "订单号，例如 ORD-1001") String orderId
    ) {
        CurrentUser user = userContext.currentUser();
        RefundAdvice result = orderSupportService.suggestRefundAction(user, orderId);
        auditLog.record(user.userId(), "suggest_refund_action", orderId, result.decision());
        return result;
    }
}
