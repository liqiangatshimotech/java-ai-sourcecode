package com.example.springaitool.tool;

import com.example.springaitool.dto.CustomerProfile;
import com.example.springaitool.dto.InventoryStatus;
import com.example.springaitool.dto.OrderStatus;
import com.example.springaitool.service.CrmService;
import com.example.springaitool.service.InventoryService;
import com.example.springaitool.service.OrderService;
import com.example.springaitool.service.ToolAuditLog;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
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
        description = "根据订单号查询订单状态、客户 ID、金额和下一步处理建议。只用于读操作，不修改订单。"
    )
    public OrderStatus queryOrderStatus(
        @ToolParam(description = "订单号，例如 O-1001") String orderId
    ) {
        OrderStatus result = this.orderService.findByOrderId(orderId);
        // 审计日志不依赖模型，由后端工具层负责，便于上线后追踪每一次工具调用。
        this.auditLog.record("query_order_status", Map.of("orderId", orderId), result.status());
        return result;
    }

    @Tool(
        name = "query_inventory",
        description = "根据 SKU 查询库存余量、仓库和是否可售。只返回库存事实，不承诺发货。"
    )
    public InventoryStatus queryInventory(
        @ToolParam(description = "商品 SKU，例如 SKU-KEYBOARD") String sku
    ) {
        InventoryStatus result = this.inventoryService.findBySku(sku);
        this.auditLog.record("query_inventory", Map.of("sku", sku), "available=" + result.availableQuantity());
        return result;
    }

    @Tool(
        name = "query_customer_profile",
        description = "根据客户 ID 查询客户等级、负责人和最近沟通备注。只用于客服辅助，不返回敏感字段。"
    )
    public CustomerProfile queryCustomerProfile(
        @ToolParam(description = "客户 ID，例如 C-2001") String customerId
    ) {
        CustomerProfile result = this.crmService.findByCustomerId(customerId);
        this.auditLog.record("query_customer_profile", Map.of("customerId", customerId), result.level());
        return result;
    }
}
