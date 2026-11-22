package com.example.enterpriseopsagent.domain;

import java.time.Instant;

/**
 * 一次线上告警的结构化输入。
 *
 * 监控平台通常会通过 Webhook 或 REST API 把告警推给后端。
 * 进入 Agent 流程之前，我会先把原始事件整理成这个对象，
 * 这样后面的日志查询、指标查询、知识召回、审批判断和数据库落库
 * 都可以使用同一份稳定的数据结构。
 */
public record OpsAlert(
    /**
     * 告警唯一编号。
     * 真实系统里可以来自 Prometheus Alertmanager、夜莺、Grafana 或内部监控平台。
     * 项目会用它做重复告警合并，避免同一故障不断创建新事件。
     */
    String alertId,

    /**
     * 触发告警的服务名。
     * 这个字段会参与日志检索、指标检索、发布记录查询和 Runbook 召回。
     */
    String serviceName,

    /**
     * 环境标识，例如 dev、test、staging、prod。
     * 生产环境的处理策略更保守：prod 的 P0、P1 告警一律先走人工审批。
     */
    String environment,

    /**
     * 告警级别，例如 P0、P1、P2、P3。
     * 级别越高，SLA 越紧：P0 是 5 分钟，P1 是 15 分钟，P2 是 30 分钟，P3 是 2 小时。
     * 级别还会影响审批：生产环境的 P0、P1 必须经过人工确认。
     */
    String severity,

    /**
     * 告警标题，适合展示在列表、通知和审批单里。
     */
    String title,

    /**
     * 告警详情，通常包含错误率、延迟、异常信息或监控表达式。
     */
    String description,

    /**
     * 告警触发时间，没传时用当前时间兜底。
     * 示例里它随事件一起落库，复盘时用来对齐时间线；
     * 日志和指标目前固定查最近 30 分钟，接入真实平台后可以改成围绕这个时间取排障窗口。
     */
    Instant triggeredAt
) {
    /**
     * 做最小必要校验，避免脏数据进入后续流程。
     * 真实项目里还会继续校验服务是否存在、当前租户是否有权限、
     * 环境是否允许自动诊断等规则。
     */
    public OpsAlert {
        if (alertId == null || alertId.isBlank()) {
            throw new IllegalArgumentException("alertId 不能为空");
        }
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName 不能为空");
        }
        if (environment == null || environment.isBlank()) {
            throw new IllegalArgumentException("environment 不能为空");
        }
        if (triggeredAt == null) {
            triggeredAt = Instant.now();
        }
    }
}
