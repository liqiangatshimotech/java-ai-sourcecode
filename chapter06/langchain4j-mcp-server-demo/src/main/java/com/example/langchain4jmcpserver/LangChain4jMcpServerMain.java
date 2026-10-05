package com.example.langchain4jmcpserver;

import dev.langchain4j.community.mcp.server.McpServer;
import dev.langchain4j.community.mcp.server.transport.StdioMcpServerTransport;

public final class LangChain4jMcpServerMain {

    private LangChain4jMcpServerMain() {
    }

    public static void main(String[] args) throws Exception {
        McpServer server = LangChain4jMcpServerFactory.demoServer();
        try (StdioMcpServerTransport transport = new StdioMcpServerTransport(server)) {
            System.err.println("LangChain4j MCP stdio server started.");
            transport.awaitClose();
        }
    }
}
