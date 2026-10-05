package com.example.enterpriseopsagent.domain;

import java.time.Instant;

/**
 * 一次线上告警的结构化输入。
 *
 * 告警平台通常会给出自然语言、JSON 或 Webhook 事件。进入 Agent 之前，
 * 我会先把它整理成 Java 对象，这样后面的检索、工具调用、权限判断和审计日志
 * 都能复用同一份数据。
 */
public record OpsAlert(
    String alertId,
    String serviceName,
    String environment,
    String severity,
    String title,
    String description,
    Instant triggeredAt
) {
    /**
     * 做最小必要校验，避免空告警进入 Agent 流程。
     * 真实项目里还会校验服务名是否存在、当前用户是否有环境访问权限。
     */
    public OpsAlert {
        if (alertId == null || alertId.isBlank()) {
            throw new IllegalArgumentException("alertId 不能为空");
        }
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName 不能为空");
        }
        if (triggeredAt == null) {
            triggeredAt = Instant.now();
        }
    }
}
