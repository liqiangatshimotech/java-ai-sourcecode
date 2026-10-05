package com.example.springaimcpserver.service;

import com.example.springaimcpserver.domain.OrderStatus;
import com.example.springaimcpserver.domain.RefundAdvice;
import com.example.springaimcpserver.security.CurrentUser;
import com.example.springaimcpserver.security.DemoUserContext;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import io.modelcontextprotocol.common.McpTransportContext;

/** MCP身份由传输上下文传入，工具参数只含业务输入。 */
@Component
public class CommerceMcpHandlers {

    private final DemoUserContext userContext;
    private final OrderService orderService;
    private final RefundPolicyService refundPolicyService;
    private final ToolAuditLog auditLog;

    public CommerceMcpHandlers(
        DemoUserContext userContext,
        OrderService orderService,
        RefundPolicyService refundPolicyService,
        ToolAuditLog auditLog
    ) {
        this.userContext = userContext;
        this.orderService = orderService;
        this.refundPolicyService = refundPolicyService;
        this.auditLog = auditLog;
    }

    @McpTool(
        name = "query_order_status",
        title = "查询订单状态",
        description = "按订单号查询当前用户有权访问的订单状态，返回结果会裁剪内部备注。",
        generateOutputSchema = true
    )
    public OrderStatus queryOrderStatus(
        @McpToolParam(description = "订单号，例如 ORD-1001") String orderId,
        McpTransportContext requestContext
    ) {
        CurrentUser user = userContext.currentUser(requestContext);
        OrderStatus status = orderService.queryOrderStatus(user, orderId);
        auditLog.record(user.userId(), "query_order_status", orderId, status.status());
        return status;
    }

    @McpTool(
        name = "suggest_refund_action",
        title = "生成退款处理建议",
        description = "根据订单状态和租户规则给出退款处理建议。只返回客服可见字段。",
        generateOutputSchema = true
    )
    public RefundAdvice suggestRefundAction(
        @McpToolParam(description = "订单号，例如 ORD-1001") String orderId,
        McpTransportContext requestContext
    ) {
        CurrentUser user = userContext.currentUser(requestContext);
        RefundAdvice advice = orderService.suggestRefundAction(user, orderId);
        auditLog.record(user.userId(), "suggest_refund_action", orderId, advice.decision());
        return advice;
    }

    @McpResource(
        uri = "policy://refund/{tenantId}",
        name = "refund-policy",
        title = "租户退款政策",
        description = "读取指定租户的退款政策。当前示例只允许读取当前用户所属租户。",
        mimeType = "text/markdown"
    )
    public ReadResourceResult readRefundPolicy(
        @McpArg(name = "tenantId", description = "租户 ID，例如 acme", required = true) String tenantId,
        McpTransportContext requestContext
    ) {
        CurrentUser user = userContext.currentUser(requestContext);
        if (!user.tenantId().equals(tenantId)) {
            throw new SecurityException("当前用户只能读取自己租户的退款政策。");
        }

        String uri = "policy://refund/" + tenantId;
        String policy = refundPolicyService.refundPolicy(tenantId);
        return new ReadResourceResult(List.of(new TextResourceContents(uri, "text/markdown", policy)));
    }

    @McpPrompt(
        name = "refund_assistant",
        title = "退款处理助手",
        description = "生成处理退款咨询的任务模板，要求模型先读订单状态和退款政策。"
    )
    public GetPromptResult refundAssistantPrompt(
        @McpArg(name = "orderId", description = "待处理订单号", required = true) String orderId,
        McpTransportContext requestContext
    ) {
        CurrentUser user = userContext.currentUser(requestContext);
        String prompt = """
            你是售后客服助手。处理退款咨询时必须遵守：
            1. 先调用 query_order_status 查询订单状态。
            2. 再读取 policy://refund/%s 获取当前租户退款政策。
            3. 如需建议处理动作，再调用 suggest_refund_action。
            4. 不得暴露内部备注，不得承诺未授权赔付。

            待处理订单：%s
            """.formatted(user.tenantId(), orderId);

        return new GetPromptResult(
            "退款处理任务模板",
            List.of(new PromptMessage(Role.USER, new TextContent(prompt)))
        );
    }
}
