package com.example.codingagent.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 一次 Coding Agent 执行会话。
 * 会话里记录任务、模型配置、审批结论和所有工具步骤，这是后续做可观测性和账单统计的基础。
 */
public final class AgentSession {

    private final String sessionId;
    private final CodingTask task;
    private final String modelSummary;
    private final String workspaceRoot;
    private final Instant createdAt;
    private final List<AgentStep> steps = new ArrayList<>();
    private ApprovalDecision approvalDecision;

    public AgentSession(String sessionId, CodingTask task, String modelSummary, String workspaceRoot) {
        this.sessionId = sessionId;
        this.task = task;
        this.modelSummary = modelSummary;
        this.workspaceRoot = workspaceRoot;
        this.createdAt = Instant.now();
    }

    public void recordStep(String name, String toolName, Instant startedAt, Instant endedAt,
                           com.example.codingagent.tool.ToolResult result) {
        steps.add(new AgentStep(steps.size() + 1, name, toolName, startedAt, endedAt, result));
    }

    public String sessionId() {
        return sessionId;
    }

    public CodingTask task() {
        return task;
    }

    public String modelSummary() {
        return modelSummary;
    }

    public String workspaceRoot() {
        return workspaceRoot;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public List<AgentStep> steps() {
        return List.copyOf(steps);
    }

    public ApprovalDecision approvalDecision() {
        return approvalDecision;
    }

    public void approvalDecision(ApprovalDecision approvalDecision) {
        this.approvalDecision = approvalDecision;
    }
}
