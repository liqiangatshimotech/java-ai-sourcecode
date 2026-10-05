package com.example.enterpriseopsagent.agent;

import com.example.enterpriseopsagent.domain.OpsAlert;
import com.example.enterpriseopsagent.tool.OpsTools;

/**
 * 只读工具执行器。
 *
 * 这里集中调用日志、指标和发布记录工具。后续如果要加权限、重试、超时、
 * 熔断或 TraceId，也可以在这一层统一处理。
 */
public class OpsToolExecutor {

    private final OpsTools tools;

    public OpsToolExecutor(OpsTools tools) {
        this.tools = tools;
    }

    public String collectReadOnlyEvidence(OpsAlert alert) {
        String logs = tools.queryLogs(alert.serviceName(), alert.environment(), "timeout", 30);
        String metrics = tools.queryMetrics(alert.serviceName(), alert.environment(), 30);
        String deployments = tools.queryRecentDeployments(alert.serviceName(), alert.environment());

        return """
            日志证据：
            %s

            指标证据：
            %s

            发布证据：
            %s
            """.formatted(logs, metrics, deployments);
    }
}
