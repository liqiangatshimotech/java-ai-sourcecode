package com.example.langchain4jtool.tool;

import com.example.langchain4jtool.dto.OrderStatus;
import com.example.langchain4jtool.service.CrmService;
import com.example.langchain4jtool.service.InventoryService;
import com.example.langchain4jtool.service.OrderService;
import com.example.langchain4jtool.service.ToolAuditLog;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void langChain4jCanBuildToolSpecificationsFromAnnotatedMethods() {
        CommerceTools tools = new CommerceTools(new OrderService(), new InventoryService(), new CrmService(), new ToolAuditLog());

        List<ToolSpecification> specifications = ToolSpecifications.toolSpecificationsFrom(tools);

        assertThat(specifications)
            .extracting(ToolSpecification::name)
            .containsExactlyInAnyOrder("query_order_status", "query_inventory", "query_customer_profile");
    }
}
