package com.example.codingagent.runtime;

import com.example.codingagent.config.ModelSettings;
import com.example.codingagent.domain.AgentSession;
import com.example.codingagent.domain.ApprovalDecision;
import com.example.codingagent.domain.CodeChangeProposal;
import com.example.codingagent.domain.CodingTask;
import com.example.codingagent.domain.RuntimeReport;
import com.example.codingagent.model.CodingModel;
import com.example.codingagent.tool.AgentTool;
import com.example.codingagent.tool.ToolContext;
import com.example.codingagent.tool.ToolRegistry;
import com.example.codingagent.tool.ToolResult;
import com.example.codingagent.workspace.WorkspaceBoundary;
import com.example.codingagent.workspace.WorkspaceSnapshot;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 一个最小但完整的 Coding Agent Runtime。
 * 它没有把模型当成万能黑盒，而是用工具、安全策略和审批服务约束模型产生的动作。
 */
public final class CodingAgentRuntime {

    private final ModelSettings settings;
    private final CodingModel model;
    private final ToolRegistry toolRegistry;
    private final ApprovalService approvalService;
    private final ToolRiskPolicy riskPolicy;
    private final AuditLogger auditLogger;

    public CodingAgentRuntime(ModelSettings settings, CodingModel model, ToolRegistry toolRegistry,
                              ApprovalService approvalService, ToolRiskPolicy riskPolicy,
                              AuditLogger auditLogger) {
        this.settings = settings;
        this.model = model;
        this.toolRegistry = toolRegistry;
        this.approvalService = approvalService;
        this.riskPolicy = riskPolicy;
        this.auditLogger = auditLogger;
    }

    /**
     * 本次任务唯一允许修改的业务文件，路径相对于演示工作区根目录，统一用 "/" 分隔。
     * 注意：PriceCalculator 不在本项目 src 里，它是 DemoWorkspaceFactory 在运行时生成到
     * target/coding-agent-workspace（命令行）或 target/coding-agent-enterprise-workspaces/coding-agent-task-<taskId>（管理台）下的演示业务仓库代码。
     */
    public static final String TARGET_PATH = "src/main/java/com/acme/billing/PriceCalculator.java";

    /** 定位目标代码时搜索的关键字：折扣上限常量。 */
    private static final String SEARCH_KEYWORD = "MAX_DISCOUNT";

    /** 创建阶段只生成可审阅提案，源码保持不变。固定场景不允许模型挑选另一个文件。 */
    public com.example.codingagent.domain.PreparedChange prepare(CodingTask task, Path root) throws IOException {
        WorkspaceSnapshot baseline = WorkspaceSnapshot.capture(root);
        String current = baseline.files().get(TARGET_PATH);
        if (current == null) throw new IllegalStateException("缺少授权的计费源文件");
        CodeChangeProposal proposal = model.proposeChange(task, TARGET_PATH, current, settings);
        if (proposal == null || !TARGET_PATH.equals(proposal.targetPath()) || proposal.newContent() == null
                || proposal.newContent().isBlank() || proposal.newContent().length() > 200_000) {
            throw new IllegalStateException("模型提案超出单文件授权范围");
        }
        var candidate = new java.util.TreeMap<>(baseline.files());
        candidate.put(TARGET_PATH, proposal.newContent());
        return new com.example.codingagent.domain.PreparedChange(UUID.randomUUID().toString(), proposal,
                baseline, sourceVersion(baseline),
                new com.example.codingagent.workspace.DiffService().diff(baseline, new WorkspaceSnapshot(candidate)));
    }

    /** 源码与测试快照同时绑定，避免审批后换源码或换掉测试。 */
    public static String sourceVersion(WorkspaceSnapshot snapshot) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            for (var entry : new java.util.TreeMap<>(snapshot.files()).entrySet()) {
                digest.update(entry.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(entry.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** CLI 演示仍可注入审批服务，但审批收到的已经是具体提案。 */
    public RuntimeReport run(CodingTask task, Path workspaceRoot) throws IOException {
        var prepared = prepare(task, workspaceRoot);
        ApprovalDecision decision = approvalService.requestApproval(task, prepared.proposal());
        return runApproved(task, workspaceRoot, prepared, decision);
    }

    /** 执行阶段只使用已保存提案，绝不再次调用模型。 */
    public RuntimeReport runApproved(CodingTask task, Path workspaceRoot,
            com.example.codingagent.domain.PreparedChange prepared, ApprovalDecision decision) throws IOException {
        Path normalizedRoot = workspaceRoot.toAbsolutePath().normalize();
        AgentSession session = new AgentSession(UUID.randomUUID().toString(), task, settings.summary(), normalizedRoot.toString());
        session.approvalDecision(decision);
        if (decision == null || !decision.approved()) {
            return new RuntimeReport(session, prepared.proposal(), false, "审批未通过，未写入代码。");
        }
        WorkspaceSnapshot actual = WorkspaceSnapshot.capture(normalizedRoot);
        if (!sourceVersion(actual).equals(prepared.sourceVersion())) {
            throw new IllegalStateException("SOURCE_VERSION_CHANGED：源码或测试已变化，需要重新生成提案并审批");
        }
        ToolContext context = new ToolContext(normalizedRoot, new WorkspaceBoundary(normalizedRoot), actual);
        auditLogger.log(session, "approved_proposal", prepared.proposalId() + " source=" + prepared.sourceVersion());
        CodeChangeProposal proposal = prepared.proposal();

        // 1. 先像人一样熟悉仓库：列文件 -> 搜关键字 -> 读目标文件，这三步都是只读的低风险工具
        ToolResult files = execute(session, context, "浏览工作区文件", "list_files", Map.of());
        if (!files.success()) throw new IllegalStateException("列出工作区文件失败");
        ToolResult search = execute(session, context, "搜索折扣上限常量", "search_code", Map.of("query", SEARCH_KEYWORD));
        String hitPath = firstSearchHitPath(search.success() ? search.output() : "");
        if (!TARGET_PATH.equals(hitPath)) {
            // 搜索定位到的文件必须就是审批过的那个文件，否则说明仓库结构和提案对不上
            throw new IllegalStateException("搜索命中的文件与已审批文件不一致：" + hitPath);
        }
        ToolResult read = execute(session, context, "读取待修改源码", "read_file", Map.of("path", TARGET_PATH));
        if (!read.success()) throw new IllegalStateException("读取目标源码失败");

        // 2. 写入审批通过的具体代码；写入是高风险工具，execute 里会再次检查审批
        ToolResult write = execute(session, context, "写入已审批的具体代码", "write_file",
                Map.of("path", proposal.targetPath(), "content", proposal.newContent()));
        if (!write.success()) throw new IllegalStateException("授权写入失败");
        // 3. 在受限容器中跑回归测试，再生成交付 diff
        ToolResult testResult = execute(session, context, "容器中执行回归测试", "run_tests", Map.of());
        ToolResult diffResult = execute(session, context, "生成交付 diff", "git_diff", Map.of());
        auditLogger.log(session, "session_finished", "testsPassed=" + testResult.success());
        return new RuntimeReport(session, proposal, testResult.success(), diffResult.output());
    }

    private ToolResult execute(AgentSession session, ToolContext context, String stepName, String toolName,
                               Map<String, String> arguments) throws IOException {
        AgentTool tool = toolRegistry.require(toolName);
        if (riskPolicy.requiresApproval(tool.risk()) && !isApproved(session.approvalDecision())) {
            throw new IllegalStateException("Tool " + toolName + " requires approval before execution");
        }

        Instant startedAt = Instant.now();
        ToolResult result = tool.execute(context, arguments);
        Instant endedAt = Instant.now();
        session.recordStep(stepName, toolName, startedAt, endedAt, result);
        auditLogger.log(session, "tool_call", toolName + " -> " + result.summary());
        return result;
    }

    private boolean isApproved(ApprovalDecision decision) {
        return decision != null && decision.approved();
    }

    private String firstSearchHitPath(String searchOutput) {
        if (searchOutput == null || searchOutput.isBlank()) {
            throw new IllegalStateException("No search result found for target code");
        }
        String firstLine = searchOutput.lines().findFirst()
                .orElseThrow(() -> new IllegalStateException("No search result found for target code"));
        int separator = firstLine.indexOf(':');
        if (separator < 0) {
            throw new IllegalStateException("Unexpected search result format: " + firstLine);
        }
        return firstLine.substring(0, separator);
    }
}
