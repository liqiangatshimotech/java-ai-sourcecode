package com.example.agentscope2.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.spec.LocalFilesystemSpec;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import java.nio.file.Path;

/**
 * 分别构建 2.0 中常用的两个 Agent 入口。
 *
 * <p>`ReActAgent` 更适合看清楚推理、工具调用和结构化输出；`HarnessAgent` 在它上面加了 workspace、memory、
 * compaction、plan mode、subagent、filesystem 等生产化能力。
 */
public final class AgentScope2AgentFactory {

    private AgentScope2AgentFactory() {}

    public static ReActAgent createReActAgent(Model model, Toolkit toolkit) {
        return ReActAgent.builder()
                .name("course-react-agent")
                .sysPrompt("你是一个企业客服工单处理助手，回答要简洁，并优先给出可执行动作。")
                .model(model)
                .toolkit(toolkit)
                .maxIters(4)
                .maxRetries(2)
                .build();
    }

    public static HarnessAgent createHarnessAgent(Model model, Path workspace, Path projectRoot) {
        return HarnessAgent.builder()
                .name("course-harness-agent")
                .sysPrompt("你在一个受控工作区里处理客服工单，重要动作先说明依据。")
                .model(model)
                .workspace(workspace)
                .filesystem(new LocalFilesystemSpec()
                        .project(projectRoot)
                        .projectWritable(false)
                        .inheritEnv(true))
                .compaction(CompactionConfig.builder()
                        .triggerMessages(30)
                        .keepMessages(10)
                        .build())
                .enablePlanMode(true)
                .disableShellTool()
                .maxRetries(2)
                .build();
    }
}
