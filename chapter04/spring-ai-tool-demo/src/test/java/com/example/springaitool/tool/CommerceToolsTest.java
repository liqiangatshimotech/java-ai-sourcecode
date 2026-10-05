package com.example.springaitool.tool;

import com.example.springaitool.config.ToolConfiguration;
import com.example.springaitool.dto.OrderStatus;
import com.example.springaitool.service.CrmService;
import com.example.springaitool.service.InventoryService;
import com.example.springaitool.service.OrderService;
import com.example.springaitool.service.ToolAuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;

import static org.assertj.core.api.Assertions.assertThat;

class CommerceToolsTest {

    @Test
    void toolMethodsReturnBusinessDataAndWriteAuditLog() {
        ToolAuditLog auditLog = new ToolAuditLog();
        CommerceTools tools = new CommerceTools(new OrderService(), new InventoryService(), new CrmService(), auditLog);

        OrderStatus order = tools.queryOrderStatus("O-1001");

        assertThat(order.status()).isEqualTo("SHIPPED");
        assertThat(auditLog.entries())
            .hasSize(1)
            .first()
            .satisfies(entry -> assertThat(entry.toolName()).isEqualTo("query_order_status"));
    }

    @Test
    void springAiCanDiscoverAnnotatedToolMethods() {
        CommerceTools tools = new CommerceTools(new OrderService(), new InventoryService(), new CrmService(), new ToolAuditLog());
        ToolCallbackProvider provider = new ToolConfiguration().commerceToolCallbacks(tools);

        assertThat(provider.getToolCallbacks())
            .extracting(callback -> callback.getToolDefinition().name())
            .containsExactlyInAnyOrder("query_order_status", "query_inventory", "query_customer_profile");
    }
}
