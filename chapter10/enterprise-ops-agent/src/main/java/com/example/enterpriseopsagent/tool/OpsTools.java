package com.example.enterpriseopsagent.tool;

import dev.langchain4j.agent.tool.Tool;

/**
 * 运维工具集合。
 *
 * 这些方法不是生产脚本，而是 Java Service 的门面。每个方法都应该在内部完成
 * 参数校验、权限校验和审计日志记录，再去访问真实系统。
 */
public class OpsTools {

    private final LogQueryService logQueryService;
    private final MetricQueryService metricQueryService;
    private final DeploymentService deploymentService;
    private final ApprovalService approvalService;

    public OpsTools(
        LogQueryService logQueryService,
        MetricQueryService metricQueryService,
        DeploymentService deploymentService,
        ApprovalService approvalService
    ) {
        this.logQueryService = logQueryService;
        this.metricQueryService = metricQueryService;
        this.deploymentService = deploymentService;
        this.approvalService = approvalService;
    }

    @Tool("查询指定服务在时间窗口内的错误日志。这个工具只允许读取日志，不允许修改生产数据。")
    public String queryLogs(String serviceName, String environment, String keyword, int minutes) {
        // 模型传入的时间窗口不能完全信任，服务端要再次限制最大查询范围。
        int safeMinutes = Math.min(Math.max(minutes, 1), 60);
        return logQueryService.searchErrorLogs(serviceName, environment, keyword, safeMinutes);
    }

    @Tool("查询指定服务的核心监控指标，例如错误率、P95 延迟、CPU 和内存。")
    public String queryMetrics(String serviceName, String environment, int minutes) {
        int safeMinutes = Math.min(Math.max(minutes, 1), 60);
        return metricQueryService.queryServiceMetrics(serviceName, environment, safeMinutes);
    }

    @Tool("查询指定服务最近的发布记录，用来判断故障是否和变更相关。")
    public String queryRecentDeployments(String serviceName, String environment) {
        return deploymentService.findRecentDeployments(serviceName, environment);
    }

    @Tool("创建回滚审批单。这个工具不会执行回滚，只会把 Agent 的建议提交给人工确认。")
    public String requestRollbackApproval(String serviceName, String environment, String reason) {
        return approvalService.createRollbackApproval(serviceName, environment, reason);
    }
}
