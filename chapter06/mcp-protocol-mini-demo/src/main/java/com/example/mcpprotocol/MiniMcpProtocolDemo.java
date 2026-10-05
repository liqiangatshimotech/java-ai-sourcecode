package com.example.mcpprotocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class MiniMcpProtocolDemo {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MiniMcpProtocolDemo() {
    }

    public static void main(String[] args) {
        MiniMcpServer server = MiniMcpServer.demoServer();

        send(server, request(1, "initialize", object()));
        send(server, notification("notifications/initialized"));
        send(server, request(2, "tools/list", object()));

        ObjectNode toolParams = object();
        toolParams.put("name", "query_order_status");
        toolParams.set("arguments", object().put("orderId", "ORD-1001"));
        send(server, request(3, "tools/call", toolParams));

        ObjectNode forbiddenToolParams = object();
        forbiddenToolParams.put("name", "query_order_status");
        forbiddenToolParams.set("arguments", object().put("orderId", "ORD-2001"));
        send(server, request(4, "tools/call", forbiddenToolParams));

        send(server, request(5, "resources/list", object()));
        send(server, request(6, "resources/read", object().put("uri", "policy://refund/acme")));
        send(server, request(7, "prompts/list", object()));

        ObjectNode promptParams = object();
        promptParams.put("name", "refund_assistant");
        promptParams.set("arguments", object().put("orderId", "ORD-1001"));
        send(server, request(8, "prompts/get", promptParams));
    }

    private static void send(MiniMcpServer server, ObjectNode request) {
        String requestJson = request.toString();
        System.out.println("CLIENT -> SERVER");
        System.out.println(pretty(requestJson));
        server.handleLine(requestJson).ifPresent(response -> {
            System.out.println("SERVER -> CLIENT");
            System.out.println(pretty(response));
        });
        System.out.println();
    }

    private static ObjectNode request(int id, String method, ObjectNode params) {
        ObjectNode request = object();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        request.set("params", params);
        return request;
    }

    private static ObjectNode notification(String method) {
        ObjectNode request = object();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        return request;
    }

    private static ObjectNode object() {
        return MAPPER.createObjectNode();
    }

    private static String pretty(String json) {
        try {
            Object parsed = MAPPER.readValue(json, Object.class);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (Exception e) {
            return json;
        }
    }
}
