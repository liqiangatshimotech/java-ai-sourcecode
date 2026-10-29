package com.example.codingagent;

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
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodingAgentRuntimeTest {

    // 这里只验证编排契约；真实 Docker 执行由独立集成验收完成。
    @Test
    void shouldRunApprovalWriteTestAndDiffFlow() throws Exception {
        Path workspace = DemoWorkspaceFactory.createFreshWorkspace(Path.of("target/coding-agent-runtime-test"));
        ToolRegistry registry = new ToolRegistry()
                .register(new ListFilesTool())
                .register(new SearchCodeTool())
                .register(new ReadFileTool())
                .register(new WriteFileTool())
                .register(new RunTestsTool((command, timeout, limit) ->
                        new com.example.codingagent.tool.ContainerRunner.Result(0, "ALL_BILLING_TESTS_PASSED (test double)", false, false)))
                .register(new GitDiffTool());

        CodingAgentRuntime runtime = new CodingAgentRuntime(
                ModelSettings.from(Map.of("AI_PROVIDER", "deepseek")),
                new RuleBasedCodingModel(),
                registry,
                new AutoApprovalService(),
                new ToolRiskPolicy(),
                new ConsoleAuditLogger()
        );

        RuntimeReport report = runtime.run(new CodingTask(
                "T-1",
                "把会员折扣上限从 20% 调整为 30%，保持方法签名不变。",
                "junit"
        ), workspace);

        String source = Files.readString(workspace.resolve("src/main/java/com/acme/billing/PriceCalculator.java"));
        assertTrue(source.contains("new BigDecimal(\"0.30\")"));
        assertTrue(report.testsPassed());
        assertTrue(report.diff().contains("0.20"));
        assertTrue(report.diff().contains("0.30"));
        assertNotNull(report.session().approvalDecision());
        // 执行轨迹要和文档一致：先定位、再写入、测试、出 diff
        assertEquals(java.util.List.of("list_files", "search_code", "read_file", "write_file", "run_tests", "git_diff"),
                report.session().steps().stream().map(com.example.codingagent.domain.AgentStep::toolName).toList());
    }

    // Windows 上 Path.toString() 用反斜杠，快照 key 必须统一成 "/" 才能匹配 TARGET_PATH
    @Test
    void shouldUseForwardSlashWorkspacePaths() {
        Path relative = Path.of("src", "main", "java", "com", "acme", "billing", "PriceCalculator.java");
        assertEquals(CodingAgentRuntime.TARGET_PATH,
                com.example.codingagent.workspace.WorkspaceSnapshot.toWorkspacePath(relative));
    }
}
