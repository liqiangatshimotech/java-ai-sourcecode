package com.example.langgraph4j.aftersales.domain;

/** 人工审批结论。PENDING 只用于状态展示，不能触发退款。 */
public enum ApprovalDecision {
    PENDING,
    APPROVED,
    REJECTED
}
