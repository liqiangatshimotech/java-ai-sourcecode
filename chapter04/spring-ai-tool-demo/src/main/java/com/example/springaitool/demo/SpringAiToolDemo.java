package com.example.springaitool.demo;

import com.example.springaitool.service.CrmService;
import com.example.springaitool.service.InventoryService;
import com.example.springaitool.service.OrderService;
import com.example.springaitool.service.ToolAuditLog;
import com.example.springaitool.tool.CommerceTools;

public class SpringAiToolDemo {

    public static void main(String[] args) {
        CommerceTools tools = new CommerceTools(
            new OrderService(),
            new InventoryService(),
            new CrmService(),
            new ToolAuditLog()
        );

        if (Boolean.parseBoolean(System.getenv().getOrDefault("RUN_MODEL_CALL","false"))) {
            var callbacks = org.springframework.ai.tool.method.MethodToolCallbackProvider.builder().toolObjects(tools).build();
            var copilot = new com.example.springaitool.service.ToolCallingCopilot(
                com.example.springaitool.config.CourseChatModelConfiguration.createBuilder(System.getenv()),callbacks);
            System.out.println(copilot.answer("查询订单 O-1001 的状态与客户 C-2001 的等级。"));
            return;
        }
        System.out.println("OFFLINE：直接验证 Java 工具，未调用模型。");
        System.out.println(tools.queryOrderStatus("O-1001"));
        System.out.println(tools.queryInventory("SKU-KEYBOARD"));
        System.out.println(tools.queryCustomerProfile("C-2001"));
    }
}
