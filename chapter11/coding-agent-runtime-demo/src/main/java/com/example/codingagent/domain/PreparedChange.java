package com.example.codingagent.domain;

import com.example.codingagent.workspace.WorkspaceSnapshot;

/**
 * 审批对象是不可变的具体提案：完整代码、原始快照、源版本摘要和预览 diff 一起保存。
 * proposalId 每次生成都不同，修复提案不能继承上一份提案的批准。
 */
public record PreparedChange(String proposalId, CodeChangeProposal proposal,
                             WorkspaceSnapshot baseline, String sourceVersion, String diff) { }
