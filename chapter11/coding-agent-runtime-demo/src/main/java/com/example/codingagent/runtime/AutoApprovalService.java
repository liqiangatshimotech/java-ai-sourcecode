package com.example.codingagent.runtime;

import com.example.codingagent.domain.ApprovalDecision;
import com.example.codingagent.domain.CodeChangeProposal;
import com.example.codingagent.domain.CodingTask;

/**
 * 演示环境用自动审批，让主流程能在本地一键跑完。
 * 生产环境不要默认自动通过，可以把这个类替换成真正的审批流。
 */
public final class AutoApprovalService implements ApprovalService {

    @Override
    public ApprovalDecision requestApproval(CodingTask task, CodeChangeProposal proposal) {
        return ApprovalDecision.approved("demo-reviewer",
                "演示环境自动审批：允许修改 " + proposal.targetPath());
    }
}
