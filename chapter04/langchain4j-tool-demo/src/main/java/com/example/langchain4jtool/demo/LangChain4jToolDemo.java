package com.example.langchain4jtool.demo;

import com.example.langchain4jtool.assistant.CommerceAssistant;
import com.example.langchain4jtool.assistant.CommerceAssistantFactory;
import com.example.langchain4jtool.service.CrmService;
import com.example.langchain4jtool.service.InventoryService;
import com.example.langchain4jtool.service.OrderService;
import com.example.langchain4jtool.service.ToolAuditLog;
import com.example.langchain4jtool.tool.CommerceTools;

public class LangChain4jToolDemo {

    public static void main(String[] args) {
        CommerceTools tools = new CommerceTools(
            new OrderService(),
            new InventoryService(),
            new CrmService(),
            new ToolAuditLog()
        );

        if (!Boolean.parseBoolean(System.getenv().getOrDefault("RUN_MODEL_CALL","false"))) {
            System.out.println("OFFLINE：直接验证 Java 工具；未调用模型，不能据此判断模型选工具能力。");
            System.out.println(tools.queryOrderStatus("O-1001"));
            System.out.println(tools.queryInventory("SKU-KEYBOARD"));
            System.out.println(tools.queryCustomerProfile("C-2001"));
            return;
        }
        CommerceAssistant assistant = CommerceAssistantFactory.createFromEnvironment(tools);
        String answer = assistant.answer("帮我查一下订单 O-1001 的状态，并看看客户 C-2001 是什么等级。");
        System.out.println(answer);
    }
}
