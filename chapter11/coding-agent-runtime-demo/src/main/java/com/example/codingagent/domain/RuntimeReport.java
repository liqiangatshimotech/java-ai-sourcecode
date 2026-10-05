package com.example.codingagent.domain;

/**
 * Runtime 的最终交付物。
 * 真实平台里通常还会包含 commit id、PR 链接、制品地址和成本明细。
 */
public record RuntimeReport(
        AgentSession session,
        CodeChangeProposal proposal,
        boolean testsPassed,
        String diff
) {
}
