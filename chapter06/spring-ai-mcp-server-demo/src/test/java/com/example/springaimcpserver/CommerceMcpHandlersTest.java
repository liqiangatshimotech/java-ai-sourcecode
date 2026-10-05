package com.example.springaimcpserver;

import com.example.springaimcpserver.domain.OrderStatus;
import com.example.springaimcpserver.domain.RefundAdvice;
import com.example.springaimcpserver.service.CommerceMcpHandlers;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CommerceMcpHandlersTest {

    @Autowired
    private CommerceMcpHandlers handlers;

    @Test
    void queriesOrderWithoutLeakingInternalNote() {
        OrderStatus status = handlers.queryOrderStatus("ORD-1001", null);

        assertEquals("ORD-1001", status.orderId());
        assertEquals("PAID", status.status());
        assertTrue(status.refundable());
        assertFalse(status.summary().contains("内部备注"));
    }

    @Test
    void blocksCrossTenantOrder() {
        assertThrows(SecurityException.class, () -> handlers.queryOrderStatus("ORD-2001", null));
    }

    @Test
    void returnsRefundPolicyResourceForCurrentTenant() {
        ReadResourceResult result = handlers.readRefundPolicy("acme", null);

        assertEquals(1, result.contents().size());
        assertEquals("policy://refund/acme", result.contents().getFirst().uri());
    }

    @Test
    void buildsRefundPrompt() {
        GetPromptResult result = handlers.refundAssistantPrompt("ORD-1001", null);

        assertEquals("退款处理任务模板", result.description());
        assertEquals(1, result.messages().size());
        assertTrue(result.messages().getFirst().content().toString().contains("query_order_status"));
    }

    @Test
    void suggestsRefundAction() {
        RefundAdvice advice = handlers.suggestRefundAction("ORD-1001", null);

        assertEquals("APPROVE", advice.decision());
        assertFalse(advice.supervisorApprovalRequired());
    }
}
