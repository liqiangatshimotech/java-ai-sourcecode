package com.example.langchain4jmcpserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.community.mcp.server.McpServer;
import dev.langchain4j.mcp.protocol.McpJsonRpcMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LangChain4jMcpServerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final McpServer server = LangChain4jMcpServerFactory.demoServer();

    @Test
    void listsAnnotatedToolsAsMcpTools() throws Exception {
        JsonNode request = mapper.readTree("""
            {"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}
            """);

        JsonNode response = toJson(server.handle(request));

        assertThat(response.path("result").path("tools")).hasSize(2);
        assertThat(response.path("result").path("tools").findValuesAsText("name"))
            .contains("query_order_status", "suggest_refund_action");
    }

    @Test
    void callsBusinessToolThroughMcpProtocol() throws Exception {
        JsonNode request = mapper.readTree("""
            {
              "jsonrpc": "2.0",
              "id": 2,
              "method": "tools/call",
              "params": {
                "name": "query_order_status",
                "arguments": {"orderId": "ORD-1001"}
              }
            }
            """);

        JsonNode response = toJson(server.handle(request));

        assertThat(response.path("result").path("isError").asBoolean(false)).isFalse();
        assertThat(response.path("result").path("content").get(0).path("text").asText())
            .contains("ORD-1001")
            .contains("PAID");
    }

    @Test
    void returnsToolErrorForInvisibleTenantOrder() throws Exception {
        JsonNode request = mapper.readTree("""
            {
              "jsonrpc": "2.0",
              "id": 3,
              "method": "tools/call",
              "params": {
                "name": "query_order_status",
                "arguments": {"orderId": "ORD-2001"}
              }
            }
            """);

        JsonNode response = toJson(server.handle(request));

        assertThat(response.path("result").path("isError").asBoolean()).isTrue();
        assertThat(response.path("result").path("content").get(0).path("text").asText())
            .contains("无权");
    }

    private JsonNode toJson(McpJsonRpcMessage response) {
        return mapper.valueToTree(response);
    }
}
