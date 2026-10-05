CREATE DATABASE IF NOT EXISTS enterprise_ops_agent
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE enterprise_ops_agent;

CREATE TABLE IF NOT EXISTS ops_incident (
    incident_id VARCHAR(64) NOT NULL COMMENT '排障事件编号',
    alert_id VARCHAR(128) NOT NULL COMMENT '外部告警 ID，用于幂等',
    service_name VARCHAR(128) NOT NULL COMMENT '服务名',
    environment VARCHAR(64) NOT NULL COMMENT '环境，例如 prod、staging',
    severity VARCHAR(32) NOT NULL COMMENT '告警等级，例如 P1、P2',
    title VARCHAR(255) NOT NULL COMMENT '告警标题',
    description TEXT NOT NULL COMMENT '告警描述',
    triggered_at TIMESTAMP(6) NOT NULL COMMENT '告警触发时间',

    report_summary TEXT NOT NULL COMMENT 'Agent 生成的故障摘要',
    root_cause_hypothesis TEXT NOT NULL COMMENT '根因假设',
    evidence MEDIUMTEXT NOT NULL COMMENT 'Runbook、日志、指标和发布记录证据',
    recommendation TEXT NOT NULL COMMENT '处理建议',
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否需要审批',

    status VARCHAR(32) NOT NULL COMMENT '事件状态',
    approval_ticket_id VARCHAR(128) NULL COMMENT '审批单号',
    sla_deadline TIMESTAMP(6) NULL COMMENT 'SLA 响应截止时间',
    created_at TIMESTAMP(6) NOT NULL COMMENT '创建时间',
    updated_at TIMESTAMP(6) NOT NULL COMMENT '更新时间',
    resolved_at TIMESTAMP(6) NULL COMMENT '关闭时间',
    duplicate_count INT NOT NULL DEFAULT 0 COMMENT '重复告警次数',

    PRIMARY KEY (incident_id),
    UNIQUE KEY uk_ops_incident_alert_id (alert_id),
    KEY idx_ops_incident_status (status),
    KEY idx_ops_incident_service_env (service_name, environment),
    KEY idx_ops_incident_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ops_incident_timeline (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '时间线自增主键',
    incident_id VARCHAR(64) NOT NULL COMMENT '排障事件编号',
    sequence_no INT NOT NULL COMMENT '同一事件内的时间线顺序',
    occurred_at TIMESTAMP(6) NOT NULL COMMENT '事件发生时间',
    event_type VARCHAR(64) NOT NULL COMMENT '时间线事件类型',
    event_operator VARCHAR(128) NOT NULL COMMENT '操作者',
    message TEXT NOT NULL COMMENT '事件说明',

    PRIMARY KEY (id),
    UNIQUE KEY uk_ops_timeline_incident_sequence (incident_id, sequence_no),
    KEY idx_ops_timeline_incident_occurred (incident_id, occurred_at),
    CONSTRAINT fk_ops_timeline_incident
        FOREIGN KEY (incident_id) REFERENCES ops_incident (incident_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
