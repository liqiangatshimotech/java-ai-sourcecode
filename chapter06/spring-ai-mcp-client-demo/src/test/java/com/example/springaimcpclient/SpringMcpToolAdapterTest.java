package com.example.springaimcpclient;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringMcpToolAdapterTest {

    @Test
    void adaptsRemoteMcpToolToSpringAiToolCallback() {
        McpSyncClient client = mock(McpSyncClient.class);
        Tool tool = Tool.builder()
            .name("query_order_status")
            .title("查询订单状态")
            .description("按订单号查询订单状态")
            .inputSchema(new JsonSchema(
                "object",
                Map.of("orderId", Map.of("type", "string")),
                List.of("orderId"),
                false,
                Map.of(),
                Map.of()
            ))
            .build();

        when(client.listTools()).thenReturn(new ListToolsResult(List.of(tool), null));
        when(client.callTool(argThat(request -> "query_order_status".equals(request.name()))))
            .thenReturn(CallToolResult.builder()
                .addTextContent("{\"orderId\":\"ORD-1001\",\"status\":\"PAID\"}")
                .isError(false)
                .build());

        SyncMcpToolCallbackProvider provider = new SyncMcpToolCallbackProvider(client);
        ToolCallback[] callbacks = provider.getToolCallbacks();
        String result = callbacks[0].call("{\"orderId\":\"ORD-1001\"}");

        assertEquals(1, callbacks.length);
        assertEquals("query_order_status", callbacks[0].getToolDefinition().name());
        assertTrue(result.contains("PAID"));
        verify(client).callTool(argThat((CallToolRequest request) ->
            "query_order_status".equals(request.name()) &&
                "ORD-1001".equals(request.arguments().get("orderId"))
        ));
    }
}
