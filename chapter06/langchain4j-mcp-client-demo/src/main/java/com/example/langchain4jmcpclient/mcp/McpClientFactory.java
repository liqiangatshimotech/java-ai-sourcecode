package com.example.langchain4jmcpclient.mcp;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;

public final class McpClientFactory {

    private McpClientFactory() {
    }

    public static McpClient stdio(McpServerConnectionProperties properties) {
        McpTransport transport = StdioMcpTransport.builder()
            .command(properties.stdioCommand())
            .environment(properties.environment())
            .logEvents(properties.logEvents())
            .build();
        return create(properties, transport);
    }

    public static McpClient streamableHttp(McpServerConnectionProperties properties) {
        McpTransport transport = StreamableHttpMcpTransport.builder()
            .url(properties.streamableHttpUrl())
            .customHeaders(properties.headers())
            .timeout(properties.toolExecutionTimeout())
            .logRequests(properties.logEvents())
            .logResponses(properties.logEvents())
            .build();
        return create(properties, transport);
    }

    private static McpClient create(McpServerConnectionProperties properties, McpTransport transport) {
        return DefaultMcpClient.builder()
            .key(properties.key())
            .clientName("java-ai-course-langchain4j-mcp-client")
            .clientVersion("0.1.0")
            .transport(transport)
            .initializationTimeout(properties.initializationTimeout())
            .toolExecutionTimeout(properties.toolExecutionTimeout())
            .cacheToolList(true)
            .autoHealthCheck(false)
            .build();
    }
}
