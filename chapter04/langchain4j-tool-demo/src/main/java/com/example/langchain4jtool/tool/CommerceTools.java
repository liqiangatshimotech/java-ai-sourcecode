package com.example.langchain4jtool.tool;

import com.example.langchain4jtool.dto.CustomerProfile;
import com.example.langchain4jtool.dto.InventoryStatus;
import com.example.langchain4jtool.dto.OrderStatus;
import com.example.langchain4jtool.service.CrmService;
import com.example.langchain4jtool.service.InventoryService;
import com.example.langchain4jtool.service.OrderService;
import com.example.langchain4jtool.service.ToolAuditLog;
import dev.langchain4j.agent.tool.Tool;

import java.util.Map;

public class CommerceTools {

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final CrmService crmService;
    private final ToolAuditLog auditLog;

    public CommerceTools(
        OrderService orderService,
        InventoryService inventoryService,
        CrmService crmService,
        ToolAuditLog auditLog
    ) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.crmService = crmService;
        this.auditLog = auditLog;
    }

    @Tool(
        name = "query_order_status",
        value = "根据订单号查询订单状态、客户 ID、金额和下一步处理建议。只用于读操作，不修改订单。"
    )
    public OrderStatus queryOrderStatus(String orderId) {
        OrderStatus result = this.orderService.findByOrderId(orderId);
        // 工具内部写审计日志，而不是依赖模型“记得记录”。
        this.auditLog.record("query_order_status", Map.of("orderId", orderId), result.status());
        return result;
    }

    @Tool(
        name = "query_inventory",
        value = "根据 SKU 查询库存余量、仓库和是否可售。只返回库存事实，不承诺发货。"
    )
    public InventoryStatus queryInventory(String sku) {
        InventoryStatus result = this.inventoryService.findBySku(sku);
        this.auditLog.record("query_inventory", Map.of("sku", sku), "available=" + result.availableQuantity());
        return result;
    }

    @Tool(
        name = "query_customer_profile",
        value = "根据客户 ID 查询客户等级、负责人和最近沟通备注。只用于客服辅助，不返回敏感字段。"
    )
    public CustomerProfile queryCustomerProfile(String customerId) {
        CustomerProfile result = this.crmService.findByCustomerId(customerId);
        this.auditLog.record("query_customer_profile", Map.of("customerId", customerId), result.level());
        return result;
    }
}
