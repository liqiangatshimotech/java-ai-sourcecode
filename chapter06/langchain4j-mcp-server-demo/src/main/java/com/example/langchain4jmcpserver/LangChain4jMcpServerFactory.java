package com.example.langchain4jmcpserver;

import com.example.langchain4jmcpserver.domain.OrderRepository;
import com.example.langchain4jmcpserver.security.DemoUserContext;
import com.example.langchain4jmcpserver.service.OrderSupportService;
import com.example.langchain4jmcpserver.service.ToolAuditLog;
import com.example.langchain4jmcpserver.tool.SupportMcpTools;
import dev.langchain4j.community.mcp.server.McpServer;
import dev.langchain4j.mcp.protocol.McpImplementation;

import java.util.List;

public final class LangChain4jMcpServerFactory {

    private LangChain4jMcpServerFactory() {
    }

    public static McpServer demoServer() {
        return create(DemoUserContext.supportAgent(), new OrderRepository(), new ToolAuditLog());
    }

    public static McpServer create(
        DemoUserContext userContext,
        OrderRepository orderRepository,
        ToolAuditLog auditLog
    ) {
        OrderSupportService orderSupportService = new OrderSupportService(orderRepository);
        SupportMcpTools tools = new SupportMcpTools(userContext, orderSupportService, auditLog);
        McpImplementation serverInfo = new McpImplementation(
            "java-ai-course-langchain4j-mcp-server",
            "0.1.0",
            "Java AI Course LangChain4j MCP Server"
        );
        return new McpServer(List.of(tools), serverInfo);
    }
}
