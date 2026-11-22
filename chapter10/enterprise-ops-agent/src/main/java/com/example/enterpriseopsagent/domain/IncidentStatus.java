package com.example.enterpriseopsagent.domain;

/**
 * 排障事件的生命周期状态。
 *
 * 真实企业项目里，Agent 生成报告只是开始，后面还要经历审批、处置和关闭。
 */
public enum IncidentStatus {
    /** 已生成排障报告，处理建议不需要审批，由值班人按建议跟进。 */
    DIAGNOSED,
    /** 处理建议需要审批，审批单已创建，等待人工决策。 */
    WAITING_APPROVAL,
    /** 审批通过，可以按建议执行处置。 */
    APPROVED,
    /** 审批被拒绝，事件到此结束，不能再关闭。 */
    REJECTED,
    /** 处置完成，事件已关闭。 */
    RESOLVED;

    /** 终态：被拒绝或已关闭的事件不再计算 SLA 超时。 */
    public boolean isClosed() {
        return this == REJECTED || this == RESOLVED;
    }
}
