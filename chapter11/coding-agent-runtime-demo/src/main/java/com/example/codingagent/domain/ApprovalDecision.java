package com.example.codingagent.domain;

import java.time.Instant;

/**
 * 高风险动作的审批结果。
 * 示例用自动审批服务，真实系统可以替换成飞书审批、企业 IM 卡片或内部工单系统。
 */
public record ApprovalDecision(
        boolean approved,
        String approver,
        String comment,
        Instant decidedAt
) {

    public static ApprovalDecision approved(String approver, String comment) {
        return new ApprovalDecision(true, approver, comment, Instant.now());
    }

    public static ApprovalDecision denied(String approver, String comment) {
        return new ApprovalDecision(false, approver, comment, Instant.now());
    }
}
