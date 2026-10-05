package com.example.mcpprotocol;

import com.example.mcpprotocol.domain.Order;
import com.example.mcpprotocol.domain.OrderRepository;
import com.example.mcpprotocol.domain.PolicyRepository;
import com.example.mcpprotocol.security.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public final class MiniMcpServer {

    public static final String PROTOCOL_VERSION = "2025-06-18";

    private static final int PARSE_ERROR = -32700;
    private static final int INVALID_REQUEST = -32600;
    private static final int METHOD_NOT_FOUND = -32601;
    private static final int INVALID_PARAMS = -32602;
    private static final int FORBIDDEN = -32001;

    private final ObjectMapper mapper;
    private final CurrentUser currentUser;
    private final OrderRepository orderRepository;
    private final PolicyRepository policyRepository;

    public MiniMcpServer(
        ObjectMapper mapper,
        CurrentUser currentUser,
        OrderRepository orderRepository,
        PolicyRepository policyRepository
    ) {
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.orderRepository = orderRepository;
        this.policyRepository = policyRepository;
    }

    public static MiniMcpServer demoServer() {
        return new MiniMcpServer(
            new ObjectMapper(),
            new CurrentUser("u-1001", "acme", Set.of("support_agent")),
            new OrderRepository(),
            new PolicyRepository()
        );
    }

    public Optional<String> handleLine(String line) {
        JsonNode request;
        try {
            request = mapper.readTree(line);
        } catch (IOException e) {
            return Optional.of(errorResponse(null, PARSE_ERROR, "Invalid JSON").toString());
        }

        if (request == null || !request.isObject() || !"2.0".equals(text(request, "jsonrpc")) || !request.hasNonNull("method")) {
            JsonNode id = request == null || request.path("id").isMissingNode() ? null : request.path("id");
            return Optional.of(errorResponse(id, INVALID_REQUEST, "JSON-RPC request must contain jsonrpc=2.0 and method").toString());
        }

        JsonNode id = request.path("id");
        String method = request.path("method").asText();

        // JSON-RPC notifications do not carry an id and must not receive a response.
        if (id.isMissingNode()) {
            handleNotification(method);
            return Optional.empty();
        }

        try {
            ObjectNode result = switch (method) {
                case "initialize" -> initializeResult();
                case "tools/list" -> toolsListResult();
                case "tools/call" -> toolsCallResult(request.path("params"));
                case "resources/list" -> resourcesListResult();
                case "resources/read" -> resourcesReadResult(request.path("params"));
                case "prompts/list" -> promptsListResult();
                case "prompts/get" -> promptsGetResult(request.path("params"));
                default -> throw new McpProtocolException(METHOD_NOT_FOUND, "Unsupported MCP method: " + method);
            };
            return Optional.of(successResponse(id, result).toString());
        } catch (McpProtocolException e) {
            return Optional.of(errorResponse(id, e.code(), e.getMessage()).toString());
        }
    }

    private void handleNotification(String method) {
        if (!"notifications/initialized".equals(method)) {
            // A real server would write this to stderr or an audit log. stdout is reserved for MCP messages.
        }
    }

    private ObjectNode initializeResult() {
        ObjectNode capabilities = object();
        capabilities.set("tools", object()
            .put("listChanged", false));
        capabilities.set("resources", object()
            .put("subscribe", false)
            .put("listChanged", false));
        capabilities.set("prompts", object()
            .put("listChanged", false));

        ObjectNode result = object();
        result.put("protocolVersion", PROTOCOL_VERSION);
        result.set("capabilities", capabilities);
        result.set("serverInfo", object()
            .put("name", "java-ai-course-mini-mcp")
            .put("version", "0.1.0"));
        return result;
    }

    private ObjectNode toolsListResult() {
        ObjectNode inputSchema = object();
        inputSchema.put("type", "object");
        inputSchema.set("properties", object()
            .set("orderId", object()
                .put("type", "string")
                .put("description", "订单号，例如 ORD-1001")));
        inputSchema.set("required", array().add("orderId"));

        ObjectNode tool = object();
        tool.put("name", "query_order_status");
        tool.put("description", "按订单号查询当前用户有权访问的订单状态。不要用它读取其他租户订单。");
        tool.set("inputSchema", inputSchema);

        ObjectNode result = object();
        result.set("tools", array().add(tool));
        return result;
    }

    private ObjectNode toolsCallResult(JsonNode params) {
        String toolName = requiredText(params, "name");
        JsonNode arguments = params.path("arguments");

        if (!"query_order_status".equals(toolName)) {
            throw new McpProtocolException(INVALID_PARAMS, "Unknown tool: " + toolName);
        }

        try {
            return queryOrderStatus(arguments);
        } catch (McpToolException e) {
            // Tool execution failures are returned as a normal MCP tool result with isError=true,
            // so the model can see the problem and decide whether to ask for more information.
            return toolTextResult(e.getMessage(), true);
        }
    }

    private ObjectNode queryOrderStatus(JsonNode arguments) {
        String orderId = requiredText(arguments, "orderId");
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new McpToolException("订单不存在：" + orderId));

        if (!order.tenantId().equals(currentUser.tenantId())) {
            throw new McpToolException("当前用户无权读取该租户订单。");
        }
        if (!currentUser.hasRole("support_agent")) {
            throw new McpToolException("当前用户缺少 support_agent 角色，不能查询订单。");
        }

        ObjectNode structured = object();
        structured.put("orderId", order.orderId());
        structured.put("customerName", order.customerName());
        structured.put("status", order.status());
        structured.put("amountYuan", order.amountCents() / 100.0);

        ObjectNode result = toolTextResult(
            "订单 %s 当前状态为 %s，金额 %.2f 元。".formatted(
                order.orderId(), order.status(), order.amountCents() / 100.0
            ),
            false
        );
        // 只返回模型完成任务需要的信息；internalNote 等敏感字段必须留在业务系统内部。
        result.set("structuredContent", structured);
        return result;
    }

    private ObjectNode resourcesListResult() {
        ObjectNode resource = object();
        resource.put("uri", "policy://refund/" + currentUser.tenantId());
        resource.put("name", "refund-policy");
        resource.put("title", "当前租户退款政策");
        resource.put("description", "客服处理退款问题前应先读取的政策上下文。");
        resource.put("mimeType", "text/markdown");
        ObjectNode result = object();
        result.set("resources", array().add(resource));
        return result;
    }

    private ObjectNode resourcesReadResult(JsonNode params) {
        String uri = requiredText(params, "uri");
        String expectedUri = "policy://refund/" + currentUser.tenantId();
        if (!expectedUri.equals(uri)) {
            throw new McpProtocolException(FORBIDDEN, "当前用户只能读取自己租户的资源：" + expectedUri);
        }

        String policy = policyRepository.refundPolicy(currentUser.tenantId())
            .orElseThrow(() -> new McpProtocolException(INVALID_PARAMS, "未配置退款政策：" + currentUser.tenantId()));

        ObjectNode content = object();
        content.put("uri", uri);
        content.put("mimeType", "text/markdown");
        content.put("text", policy);
        ObjectNode result = object();
        result.set("contents", array().add(content));
        return result;
    }

    private ObjectNode promptsListResult() {
        ObjectNode prompt = object();
        prompt.put("name", "refund_assistant");
        prompt.put("title", "退款处理助手");
        prompt.put("description", "生成客服退款处理建议，要求先读取订单和退款政策。");
        prompt.set("arguments", array().add(object()
            .put("name", "orderId")
            .put("description", "待处理订单号")
            .put("required", true)));
        ObjectNode result = object();
        result.set("prompts", array().add(prompt));
        return result;
    }

    private ObjectNode promptsGetResult(JsonNode params) {
        String name = requiredText(params, "name");
        if (!"refund_assistant".equals(name)) {
            throw new McpProtocolException(INVALID_PARAMS, "Unknown prompt: " + name);
        }

        String orderId = requiredText(params.path("arguments"), "orderId");
        ObjectNode textContent = object();
        textContent.put("type", "text");
        textContent.put("text", """
            你是售后客服助手。回答前必须：
            1. 调用 query_order_status 工具读取订单状态。
            2. 读取 policy://refund/%s 资源获取退款政策。
            3. 不得承诺未授权赔付，不得暴露内部备注。

            待处理订单：%s
            """.formatted(currentUser.tenantId(), orderId));

        ObjectNode message = object();
        message.put("role", "user");
        message.set("content", textContent);

        ObjectNode result = object();
        result.put("description", "退款处理任务模板");
        result.set("messages", array().add(message));
        return result;
    }

    private ObjectNode toolTextResult(String text, boolean isError) {
        ObjectNode content = object();
        content.put("type", "text");
        content.put("text", text);

        ObjectNode result = object();
        result.set("content", array().add(content));
        result.put("isError", isError);
        return result;
    }

    private String requiredText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new McpProtocolException(INVALID_PARAMS, "Missing required string parameter: " + fieldName);
        }
        return value.asText();
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isTextual() ? value.asText() : null;
    }

    private ObjectNode successResponse(JsonNode id, ObjectNode result) {
        ObjectNode response = object();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        return response;
    }

    private ObjectNode errorResponse(JsonNode id, int code, String message) {
        ObjectNode error = object();
        error.put("code", code);
        error.put("message", message);

        ObjectNode response = object();
        response.put("jsonrpc", "2.0");
        if (id == null) {
            response.putNull("id");
        } else {
            response.set("id", id);
        }
        response.set("error", error);
        return response;
    }

    private ObjectNode object() {
        return mapper.createObjectNode();
    }

    private ArrayNode array() {
        return mapper.createArrayNode();
    }
}
