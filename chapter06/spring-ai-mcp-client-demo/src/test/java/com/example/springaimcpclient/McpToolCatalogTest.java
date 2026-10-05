package com.example.springaimcpclient;

import com.example.springaimcpclient.service.McpToolCatalog;
import com.example.springaimcpclient.web.McpToolView;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolCatalogTest {

    @Test
    void exposesToolDefinitionsForUiAndDiagnostics() {
        ToolCallback callback = new StaticToolCallback();
        McpToolCatalog catalog = new McpToolCatalog(new FixedObjectProvider(ToolCallbackProvider.from(callback)));

        List<McpToolView> tools = catalog.tools();

        assertEquals(1, tools.size());
        assertEquals("query_order_status", tools.getFirst().name());
        assertEquals("{\"type\":\"object\"}", tools.getFirst().inputSchema());
    }

    private static class StaticToolCallback implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("query_order_status")
                .description("按订单号查询订单状态")
                .inputSchema("{\"type\":\"object\"}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return "{\"status\":\"PAID\"}";
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            return call(toolInput);
        }
    }

    private record FixedObjectProvider(ToolCallbackProvider provider) implements ObjectProvider<ToolCallbackProvider> {

        @Override
        public ToolCallbackProvider getObject(Object... args) {
            return provider;
        }

        @Override
        public ToolCallbackProvider getIfAvailable() {
            return provider;
        }

        @Override
        public ToolCallbackProvider getIfUnique() {
            return provider;
        }

        @Override
        public ToolCallbackProvider getObject() {
            return provider;
        }
    }
}
