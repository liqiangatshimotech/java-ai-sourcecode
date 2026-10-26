package com.example.agentruntime.tool;

import com.example.agentruntime.domain.AgentRunRequest;
/** allowWrite 仅用于本地演示；生产应改为服务端验证后的授权上下文。 */
public final class ToolPermissionGuard {
    public ToolAuthorization authorize(AgentRunRequest request, ToolDefinition tool) {
        if (tool.risk() == ToolRisk.WRITE_REQUIRES_CONFIRMATION && !request.allowWrite()) {
            return ToolAuthorization.blocked("工具 " + tool.name() + " 需要人工确认后才能执行");
        }
        return ToolAuthorization.permitted();
    }
}
