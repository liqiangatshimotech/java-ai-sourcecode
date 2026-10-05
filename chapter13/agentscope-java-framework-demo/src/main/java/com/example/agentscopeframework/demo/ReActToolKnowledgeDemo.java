package com.example.agentscopeframework.demo;

import com.example.agentscopeframework.tool.CustomerTicketTools;
import io.agentscope.core.tool.Toolkit;

/**
 * 知识点 13.2：ReAct Agent 和工具调用。
 *
 * <p>这里重点看 Java 业务工具怎么被放进 Toolkit。真实对话中，ReActAgent 会根据用户请求决定是否调用这些工具。
 */
public class ReActToolKnowledgeDemo {

    public static void main(String[] args) {
        Toolkit toolkit = new Toolkit();
        CustomerTicketTools tools = new CustomerTicketTools();
        toolkit.registerTool(tools);

        System.out.println("已注册客服工具到 Toolkit。");
        System.out.println(tools.queryOrderStatus("ORDER-2002"));
        System.out.println(tools.searchPolicy("refund"));
        System.out.println(tools.createEscalation("TICKET-1", "VIP 用户库存不足并要求退款"));
    }
}
