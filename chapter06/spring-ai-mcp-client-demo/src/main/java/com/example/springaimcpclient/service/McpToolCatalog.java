package com.example.springaimcpclient.service;

import com.example.springaimcpclient.web.McpToolView;
import java.util.Arrays;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class McpToolCatalog {

    private final ToolCallbackProvider mcpToolCallbacks;

    public McpToolCatalog(ObjectProvider<ToolCallbackProvider> mcpToolCallbacksProvider) {
        this.mcpToolCallbacks = mcpToolCallbacksProvider.getIfAvailable(ToolCallbackProvider::from);
    }

    public List<McpToolView> tools() {
        return Arrays.stream(mcpToolCallbacks.getToolCallbacks())
            .map(this::toView)
            .toList();
    }

    private McpToolView toView(ToolCallback callback) {
        return new McpToolView(
            callback.getToolDefinition().name(),
            callback.getToolDefinition().description(),
            callback.getToolDefinition().inputSchema()
        );
    }
}
