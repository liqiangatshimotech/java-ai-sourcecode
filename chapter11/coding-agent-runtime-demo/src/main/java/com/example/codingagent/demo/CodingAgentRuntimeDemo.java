package com.example.codingagent.demo;

import com.example.codingagent.config.ModelSettings;
import com.example.codingagent.domain.CodingTask;
import com.example.codingagent.domain.RuntimeReport;
import com.example.codingagent.model.RuleBasedCodingModel;
import com.example.codingagent.runtime.AutoApprovalService;
import com.example.codingagent.runtime.CodingAgentRuntime;
import com.example.codingagent.runtime.ConsoleAuditLogger;
import com.example.codingagent.runtime.ToolRiskPolicy;
import com.example.codingagent.tool.GitDiffTool;
import com.example.codingagent.tool.ListFilesTool;
import com.example.codingagent.tool.ReadFileTool;
import com.example.codingagent.tool.RunTestsTool;
import com.example.codingagent.tool.SearchCodeTool;
import com.example.codingagent.tool.ToolRegistry;
import com.example.codingagent.tool.WriteFileTool;
import com.example.codingagent.workspace.DemoWorkspaceFactory;

import java.nio.file.Path;

/**
 * 一键运行入口。
 * 运行后可以在 target/coding-agent-workspace 里看到 Agent 修改过的演示业务代码。
 */
public final class CodingAgentRuntimeDemo {

    private CodingAgentRuntimeDemo() {
    }

    public static void main(String[] args) throws Exception {
        Path workspace = DemoWorkspaceFactory.createFreshWorkspace(Path.of("target/coding-agent-workspace"));
        ModelSettings settings = ModelSettings.fromEnvironment();

        ToolRegistry registry = new ToolRegistry()
                .register(new ListFilesTool())
                .register(new SearchCodeTool())
                .register(new ReadFileTool())
                .register(new WriteFileTool())
                .register(new RunTestsTool())
                .register(new GitDiffTool());

        CodingAgentRuntime runtime = new CodingAgentRuntime(
                settings,
                new RuleBasedCodingModel(),
                registry,
                new AutoApprovalService(),
                new ToolRiskPolicy(),
                new ConsoleAuditLogger()
        );

        CodingTask task = new CodingTask(
                "TASK-11-001",
                "把会员折扣上限从 20% 调整为 30%，同时不要修改 calculateFinalPrice 的方法签名。",
                "course-demo"
        );

        RuntimeReport report = runtime.run(task, workspace);
        System.out.println();
        System.out.println("=== Runtime Summary ===");
        System.out.println("model: " + report.session().modelSummary());
        System.out.println("approval: " + report.session().approvalDecision().comment());
        System.out.println("testsPassed: " + report.testsPassed());
        System.out.println();
        System.out.println(report.diff());
    }
}
