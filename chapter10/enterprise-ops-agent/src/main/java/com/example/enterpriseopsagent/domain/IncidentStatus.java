package com.example.enterpriseopsagent.domain;

/**
 * 排障事件的生命周期状态。
 *
 * 真实企业项目里，Agent 生成报告只是开始，后面还要经历审批、处置和关闭。
 */
public enum IncidentStatus {
    DIAGNOSED,
    WAITING_APPROVAL,
    APPROVED,
    REJECTED,
    RESOLVED;

    public boolean isClosed() {
        return this == REJECTED || this == RESOLVED;
    }
}
