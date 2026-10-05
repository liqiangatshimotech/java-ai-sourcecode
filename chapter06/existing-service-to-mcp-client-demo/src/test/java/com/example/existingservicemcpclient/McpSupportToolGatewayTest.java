package com.example.existingservicemcpclient;

import com.example.existingservicemcpclient.existing.OrderStatusSnapshot;
import com.example.existingservicemcpclient.existing.RefundActionSnapshot;
import com.example.existingservicemcpclient.mcp.McpSupportToolGateway;
import com.example.existingservicemcpclient.mcp.McpToolCallException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpSupportToolGatewayTest {

    @Test
    void callsRemoteMcpToolsThroughSpringAiToolCallbacks() {
        McpSupportToolGateway gateway = new McpSupportToolGateway(
            ToolCallbackProvider.from(
                new StaticJsonTool("query_order_status", """
                    {"orderId":"ORD-1001","status":"PAID","refundable":true,"summary":"订单 ORD-1001 当前状态为 PAID。"}
                    """),
                new StaticJsonTool("suggest_refund_action", """
                    {"orderId":"ORD-1001","decision":"APPROVE","supervisorApprovalRequired":false,"reason":"未发货且符合退款规则。"}
                    """)
            ),
            new ObjectMapper()
        );

        OrderStatusSnapshot order = gateway.queryOrderStatus("ORD-1001");
        RefundActionSnapshot advice = gateway.suggestRefundAction("ORD-1001");

        assertEquals("PAID", order.status());
        assertEquals("APPROVE", advice.decision());
    }

    @Test
    void unwrapsMcpTextContentReturnedBySpringAiCallbacks() {
        McpSupportToolGateway gateway = new McpSupportToolGateway(
            ToolCallbackProvider.from(
                new StaticJsonTool("query_order_status", """
                    [{"type":"text","text":"{\\"orderId\\":\\"ORD-1001\\",\\"status\\":\\"PAID\\",\\"refundable\\":true,\\"summary\\":\\"ok\\"}"}]
                    """)
            ),
            new ObjectMapper()
        );

        assertEquals("PAID", gateway.queryOrderStatus("ORD-1001").status());
    }

    @Test
    void failsFastWhenExpectedToolIsMissing() {
        McpSupportToolGateway gateway = new McpSupportToolGateway(
            ToolCallbackProvider.from(),
            new ObjectMapper()
        );

        assertThrows(McpToolCallException.class, () -> gateway.queryOrderStatus("ORD-1001"));
    }

    private record StaticJsonTool(String name, String result) implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name(name)
                .description("static test tool")
                .inputSchema("{\"type\":\"object\"}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return result;
        }
    }
}
