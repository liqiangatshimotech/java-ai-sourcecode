package com.example.enterpriseopsagent.domain;

/**
 * 人工审批决策。
 *
 * Agent 可以建议回滚，但生产动作必须经过明确的人为决策。
 */
public enum DecisionType {
    APPROVE,
    REJECT
}
