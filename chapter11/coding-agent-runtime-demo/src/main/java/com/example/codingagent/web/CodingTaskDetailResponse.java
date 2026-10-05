package com.example.codingagent.web;

import com.example.codingagent.domain.AgentStep;
import com.example.codingagent.domain.ApprovalDecision;
import com.example.codingagent.domain.RuntimeReport;
import com.example.codingagent.service.CodingAgentJob;
import com.example.codingagent.service.CodingTaskStatus;

import java.time.Instant;
import java.util.List;

public record CodingTaskDetailResponse(
        String taskId,
        String repositoryName,
        String branchName,
        String description,
        String operator,
        CodingTaskStatus status,
        Instant createdAt,
        Instant updatedAt,
        String workspaceRoot,
        ApprovalView approval,
        ProposalView proposal,
        boolean testsPassed,
        String diff,
        String errorMessage,
        List<ToolStepView> steps,
        int repairAttempts,
        List<String> auditEvents
) {

    public static CodingTaskDetailResponse from(CodingAgentJob job) {
        RuntimeReport report = job.report();
        return new CodingTaskDetailResponse(
                job.taskId(),
                job.repositoryName(),
                job.branchName(),
                job.description(),
                job.operator(),
                job.status(),
                job.createdAt(),
                job.updatedAt(),
                job.workspaceRoot().toString(),
                ApprovalView.from(job.approvalDecision()),
                ProposalView.from(job.prepared()),
                report != null && report.testsPassed(),
                job.prepared() == null ? "" : job.prepared().diff(),
                job.errorMessage(),
                report == null ? List.of() : report.session().steps().stream().map(ToolStepView::from).toList(),
                job.repairAttempts(),
                job.auditEvents()
        );
    }

    public record ApprovalView(boolean approved, String approver, String comment, Instant decidedAt) {

        public static ApprovalView from(ApprovalDecision decision) {
            if (decision == null) {
                return new ApprovalView(false, "", "", null);
            }
            return new ApprovalView(decision.approved(), decision.approver(), decision.comment(), decision.decidedAt());
        }
    }

    public record ProposalView(String proposalId, String sourceVersion, String targetPath,
                               String rationale, String newContent) {
        public static ProposalView from(com.example.codingagent.domain.PreparedChange prepared) {
            if (prepared == null) return new ProposalView("", "", "", "", "");
            return new ProposalView(prepared.proposalId(), prepared.sourceVersion(), prepared.proposal().targetPath(),
                    prepared.proposal().rationale(), prepared.proposal().newContent());
        }
    }

    public record ToolStepView(int index, String name, String toolName, boolean success, String summary,
                               String output, Instant startedAt, Instant endedAt) {

        public static ToolStepView from(AgentStep step) {
            return new ToolStepView(
                    step.index(),
                    step.name(),
                    step.toolName(),
                    step.result().success(),
                    step.result().summary(),
                    step.result().output(),
                    step.startedAt(),
                    step.endedAt()
            );
        }
    }
}
