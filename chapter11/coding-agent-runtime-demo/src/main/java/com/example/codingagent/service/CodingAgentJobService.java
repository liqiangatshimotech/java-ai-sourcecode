package com.example.codingagent.service;

import com.example.codingagent.config.ModelSettings;
import com.example.codingagent.domain.ApprovalDecision;
import com.example.codingagent.domain.CodingTask;
import com.example.codingagent.domain.RuntimeReport;
import com.example.codingagent.model.RuleBasedCodingModel;
import com.example.codingagent.runtime.ApprovalService;
import com.example.codingagent.runtime.AuditLogger;
import com.example.codingagent.runtime.CodingAgentRuntime;
import com.example.codingagent.runtime.ToolRiskPolicy;
import com.example.codingagent.tool.GitDiffTool;
import com.example.codingagent.tool.ListFilesTool;
import com.example.codingagent.tool.ReadFileTool;
import com.example.codingagent.tool.RunTestsTool;
import com.example.codingagent.tool.SearchCodeTool;
import com.example.codingagent.tool.ToolRegistry;
import com.example.codingagent.tool.WriteFileTool;
import com.example.codingagent.workspace.DemoWorkspaceFactory;
import com.example.codingagent.workspace.WorkspaceBoundary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coding Agent 任务中心。
 * 这里模拟企业系统里的任务表、审批流和执行编排，UI 与 REST API 都围绕这个服务工作。
 */
@Service
public class CodingAgentJobService {

    private static final DateTimeFormatter ID_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.systemDefault());

    private final AtomicInteger sequence = new AtomicInteger(1000);
    private final Map<String, CodingAgentJob> jobs = new ConcurrentHashMap<>();
    private final Path workspaceBase;
    private final com.example.codingagent.model.CodingModel model;
    private final ToolRegistry tools;
    private final ModelSettings settings;

    public CodingAgentJobService() {
        this(Path.of("target/coding-agent-enterprise-workspaces"),
            com.example.codingagent.model.HttpCodingModel.fromEnvironment(), defaultToolRegistry(), ModelSettings.fromEnvironment());
    }

    /** 显式依赖入口供契约测试使用；测试桩不会伪装成真实容器验收。 */
    public CodingAgentJobService(Path workspaceBase, com.example.codingagent.model.CodingModel model,
                                ToolRegistry tools, ModelSettings settings) {
        this.workspaceBase = workspaceBase.toAbsolutePath().normalize();
        this.model = model;
        this.tools = tools;
        this.settings = settings;
    }

    public CodingAgentJob createJob(String repositoryName, String branchName, String description,
                                    String operator) throws IOException {
        String taskId = "CA-" + ID_TIME.format(Instant.now()) + "-" + sequence.incrementAndGet();
        Path workspaceRoot = workspaceBase.resolve("coding-agent-task-" + taskId.toLowerCase(Locale.ROOT));
        DemoWorkspaceFactory.createFreshWorkspace(workspaceRoot);
        CodingAgentJob job = new CodingAgentJob(
                taskId,
                firstNonBlank(repositoryName, "billing-service"),
                firstNonBlank(branchName, "feature/member-discount-limit"),
                description,
                firstNonBlank(operator, "course-demo"),
                workspaceRoot
        );
        jobs.put(taskId, job);
        synchronized (job) {
            try { job.installProposal(buildRuntime(job).prepare(task(job, job.description()), workspaceRoot)); }
            catch (Exception failure) { job.markFailed("提案生成失败：" + failure.getMessage()); }
        }
        return job;
    }

    public CodingAgentJob approve(String taskId, String proposalId, String sourceVersion,
                                  String approver, String comment) throws IOException {
        CodingAgentJob job = requireJob(taskId);
        synchronized (job) {
            requireProposal(job, proposalId);
            if (!job.prepared().sourceVersion().equals(sourceVersion)
                    || !sourceVersion.equals(CodingAgentRuntime.sourceVersion(
                        com.example.codingagent.workspace.WorkspaceSnapshot.capture(job.workspaceRoot())))) {
                throw new IllegalStateException("SOURCE_VERSION_CHANGED：请重新创建任务");
            }
            if (job.status() == CodingTaskStatus.APPROVED) return job;
            if (job.status() != CodingTaskStatus.WAITING_APPROVAL)
                throw new IllegalStateException("只有等待审批的当前提案可以批准");
            job.approve(ApprovalDecision.approved(firstNonBlank(approver, "reviewer"),
                    firstNonBlank(comment, "批准当前提案与源版本")));
            return job;
        }
    }

    public CodingAgentJob run(String taskId, String proposalId) {
        CodingAgentJob job = requireJob(taskId);
        synchronized (job) {
            // 同一提案的重复执行是幂等读取；不能在修复版本已批准时执行另一个版本。
            if (job.consumed(proposalId)) return job;
            requireProposal(job, proposalId);
            if (job.status() != CodingTaskStatus.APPROVED)
                throw new IllegalStateException("当前提案必须先审批通过");
            job.consume(proposalId);
            job.markRunning();
            try {
                RuntimeReport report = buildRuntime(job).runApproved(task(job, job.description()),
                        job.workspaceRoot(), job.prepared(), job.approvalDecision());
                job.markCompleted(report);
            } catch (Exception exception) { job.markFailed(exception.getMessage()); }
            return job;
        }
    }

    /** 测试失败最多生成一份修复提案；新版本停在 WAITING_APPROVAL，不继承批准。 */
    public CodingAgentJob repair(String taskId, String failedProposalId) {
        CodingAgentJob job = requireJob(taskId);
        synchronized (job) {
            requireProposal(job, failedProposalId);
            if (job.status() != CodingTaskStatus.FAILED || job.report() == null || job.repairAttempts() >= 1)
                throw new IllegalStateException("只允许对已执行失败的测试生成一次修复提案");
            String failure = job.report().session().steps().stream()
                    .filter(step -> step.toolName().equals("run_tests") && !step.result().success()
                        && step.result().summary().equals("TESTS_FAILED"))
                    .map(step -> step.result().output()).findFirst()
                    .orElseThrow(() -> new IllegalStateException("容器环境不可用时应先修复环境，不能让模型改代码掩盖故障"));
            job.startRepair();
            try {
                String description = job.description() + "\n上一份提案测试失败，请针对同一文件修复。\n受限测试反馈：\n"
                        + failure.substring(0, Math.min(failure.length(), 3000));
                job.installProposal(buildRuntime(job).prepare(task(job, description), job.workspaceRoot()));
            } catch (Exception ex) { job.markFailed("修复提案生成失败：" + ex.getMessage()); }
            return job;
        }
    }

    private CodingTask task(CodingAgentJob job, String description) {
        return new CodingTask(job.taskId(), description, job.operator());
    }

    private void requireProposal(CodingAgentJob job, String proposalId) {
        if (job.prepared() == null || !job.prepared().proposalId().equals(proposalId))
            throw new IllegalStateException("PROPOSAL_CHANGED：请重新查看当前提案");
    }

    public List<CodingAgentJob> listJobs() {
        return jobs.values().stream()
                .sorted(Comparator.comparing(CodingAgentJob::createdAt).reversed())
                .toList();
    }

    public CodingAgentJob requireJob(String taskId) {
        CodingAgentJob job = jobs.get(taskId);
        if (job == null) {
            throw new CodingAgentJobNotFoundException(taskId);
        }
        return job;
    }

    public String readWorkspaceFile(String taskId, String relativePath) throws IOException {
        CodingAgentJob job = requireJob(taskId);
        Path path = new WorkspaceBoundary(job.workspaceRoot()).resolve(relativePath);
        return Files.readString(path);
    }

    public List<String> registeredToolNames() {
        return tools.names();
    }

    public Path workspaceBase() {
        return workspaceBase;
    }

    private CodingAgentRuntime buildRuntime(CodingAgentJob job) {
        ApprovalDecision approved = job.approvalDecision();
        ApprovalService approvalService = (task, proposal) -> approved;
        AuditLogger auditLogger = (session, event, detail) -> job.addAudit(event + " | " + detail);
        return new CodingAgentRuntime(
                settings,
                model,
                tools,
                approvalService,
                new ToolRiskPolicy(),
                auditLogger
        );
    }

    private static ToolRegistry defaultToolRegistry() {
        return new ToolRegistry()
                .register(new ListFilesTool())
                .register(new SearchCodeTool())
                .register(new ReadFileTool())
                .register(new WriteFileTool())
                .register(new RunTestsTool())
                .register(new GitDiffTool());
    }

    private String firstNonBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
