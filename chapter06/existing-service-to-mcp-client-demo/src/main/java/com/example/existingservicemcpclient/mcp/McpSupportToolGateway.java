package com.example.existingservicemcpclient.mcp;

import com.example.existingservicemcpclient.existing.OrderStatusSnapshot;
import com.example.existingservicemcpclient.existing.RefundActionSnapshot;
import com.example.existingservicemcpclient.existing.RemoteSupportToolGateway;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class McpSupportToolGateway implements RemoteSupportToolGateway {

    private final ToolCallbackProvider toolCallbacks;

    private final ObjectMapper objectMapper;

    @Autowired
    public McpSupportToolGateway(ObjectProvider<ToolCallbackProvider> toolCallbacks, ObjectMapper objectMapper) {
        this(toolCallbacks.getIfAvailable(() -> ToolCallbackProvider.from()), objectMapper);
    }

    public McpSupportToolGateway(ToolCallbackProvider toolCallbacks, ObjectMapper objectMapper) {
        this.toolCallbacks = toolCallbacks;
        this.objectMapper = objectMapper;
    }

    @Override
    public OrderStatusSnapshot queryOrderStatus(String orderId) {
        return callTool(
            "query_order_status",
            Map.of("orderId", orderId),
            OrderStatusSnapshot.class
        );
    }

    @Override
    public RefundActionSnapshot suggestRefundAction(String orderId) {
        return callTool(
            "suggest_refund_action",
            Map.of("orderId", orderId),
            RefundActionSnapshot.class
        );
    }

    private <T> T callTool(String toolName, Map<String, Object> input, Class<T> resultType) {
        ToolCallback callback = findTool(toolName);
        try {
            String toolInput = objectMapper.writeValueAsString(input);
            String toolResult = callback.call(toolInput);
            return objectMapper.readValue(unwrapTextContent(toolResult), resultType);
        }
        catch (JsonProcessingException ex) {
            throw new McpToolCallException("MCP tool returned invalid JSON: " + toolName, ex);
        }
    }

    // Spring AI 的 MCP ToolCallback 返回的是 MCP content 列表，例如
    // [{"type":"text","text":"{\"orderId\":\"ORD-1001\"}"}]，业务 JSON 在 text 字段里。
    // 这里把第一段 text 取出来；如果已经是普通 JSON 对象，就原样返回。
    private String unwrapTextContent(String toolResult) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(toolResult);
        if (!root.isArray()) {
            return toolResult;
        }
        for (JsonNode content : root) {
            if (content.hasNonNull("text")) {
                return content.get("text").asText();
            }
        }
        return toolResult;
    }

    private ToolCallback findTool(String toolName) {
        return Arrays.stream(toolCallbacks.getToolCallbacks())
            .filter(callback -> toolName.equals(callback.getToolDefinition().name()))
            .findFirst()
            .orElseThrow(() -> new McpToolCallException("MCP tool is not available: " + toolName));
    }
}
