package com.example.enterpriseopsagent.tool;

/**
 * Demo 版运维服务实现。
 *
 * 这些实现不连接真实系统，只返回固定样例，方便先跑通 Agent 的业务闭环。
 * 后续接入企业系统时，替换这些接口实现即可。
 */
public final class InMemoryOpsServices {

    private InMemoryOpsServices() {
    }

    public static LogQueryService logs() {
        return (serviceName, environment, keyword, minutes) ->
            "最近 " + minutes + " 分钟内，" + environment + " 环境的 " + serviceName
                + " 出现 48 条包含 " + keyword + " 的错误日志，主要集中在支付回调超时。";
    }

    public static MetricQueryService metrics() {
        return (serviceName, environment, minutes) ->
            serviceName + " 在 " + environment + " 环境最近 " + minutes
                + " 分钟 P95 延迟从 180ms 上升到 2300ms，错误率从 0.2% 上升到 7.8%。";
    }

    public static DeploymentService deployments() {
        return (serviceName, environment) ->
            serviceName + " 在 " + environment + " 环境 18 分钟前发布 v2.7.3，变更包含支付回调超时时间调整。";
    }

    public static ApprovalService approvals() {
        return (serviceName, environment, reason) ->
            "APPROVAL-" + serviceName + "-" + environment + "-ROLLBACK";
    }
}
