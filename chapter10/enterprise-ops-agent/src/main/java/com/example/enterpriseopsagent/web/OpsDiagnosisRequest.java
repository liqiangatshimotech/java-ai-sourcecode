package com.example.enterpriseopsagent.web;

import com.example.enterpriseopsagent.domain.OpsAlert;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * HTTP 层的告警诊断请求。
 *
 * Controller 接收的是外部系统传来的 JSON，进入业务层之前转成 OpsAlert。
 * 这样 HTTP 字段校验和业务对象校验可以各自保持清晰。
 */
public record OpsDiagnosisRequest(
    @NotBlank(message = "alertId 不能为空")
    String alertId,

    @NotBlank(message = "serviceName 不能为空")
    String serviceName,

    @NotBlank(message = "environment 不能为空")
    String environment,

    @NotBlank(message = "severity 不能为空")
    String severity,

    @NotBlank(message = "title 不能为空")
    String title,

    @NotBlank(message = "description 不能为空")
    String description,

    Instant triggeredAt
) {
    public OpsAlert toOpsAlert() {
        return new OpsAlert(
            alertId,
            serviceName,
            environment,
            severity,
            title,
            description,
            triggeredAt
        );
    }
}
