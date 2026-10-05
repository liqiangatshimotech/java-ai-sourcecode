package com.example.mcpprotocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniMcpServerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final MiniMcpServer server = MiniMcpServer.demoServer();

    @Test
    void listsToolsWithInputSchema() throws Exception {
        JsonNode response = send("""
            {"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}
            """);

        assertEquals("query_order_status", response.at("/result/tools/0/name").asText());
        assertEquals("object", response.at("/result/tools/0/inputSchema/type").asText());
        assertEquals("orderId", response.at("/result/tools/0/inputSchema/required/0").asText());
    }

    @Test
    void callsToolAndCutsSensitiveFields() throws Exception {
        JsonNode response = send("""
            {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"query_order_status","arguments":{"orderId":"ORD-1001"}}}
            """);

        assertFalse(response.at("/result/isError").asBoolean());
        assertEquals("ORD-1001", response.at("/result/structuredContent/orderId").asText());
        assertTrue(response.at("/result/structuredContent/internalNote").isMissingNode());
    }

    @Test
    void returnsToolErrorForForbiddenOrder() throws Exception {
        JsonNode response = send("""
            {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"query_order_status","arguments":{"orderId":"ORD-2001"}}}
            """);

        assertTrue(response.at("/result/isError").asBoolean());
        assertTrue(response.at("/result/content/0/text").asText().contains("无权"));
    }

    @Test
    void readsTenantResourceOnly() throws Exception {
        JsonNode response = send("""
            {"jsonrpc":"2.0","id":4,"method":"resources/read","params":{"uri":"policy://refund/acme"}}
            """);

        assertEquals("policy://refund/acme", response.at("/result/contents/0/uri").asText());
        assertTrue(response.at("/result/contents/0/text").asText().contains("ACME 退款政策"));
    }

    @Test
    void doesNotRespondToNotifications() {
        Optional<String> response = server.handleLine("""
            {"jsonrpc":"2.0","method":"notifications/initialized"}
            """);

        assertTrue(response.isEmpty());
    }

    private JsonNode send(String request) throws Exception {
        String response = server.handleLine(request).orElseThrow();
        return mapper.readTree(response);
    }
}
