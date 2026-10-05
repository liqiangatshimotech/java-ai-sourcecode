package com.example.enterpriseopsagent.demo;

import com.example.enterpriseopsagent.agent.ConsoleAuditLogService;
import com.example.enterpriseopsagent.agent.OpsAgentService;
import com.example.enterpriseopsagent.agent.OpsToolExecutor;
import com.example.enterpriseopsagent.agent.RuleBasedDiagnosisModel;
import com.example.enterpriseopsagent.config.ModelSettings;
import com.example.enterpriseopsagent.domain.DiagnosisReport;
import com.example.enterpriseopsagent.domain.OpsAlert;
import com.example.enterpriseopsagent.tool.InMemoryOpsServices;
import com.example.enterpriseopsagent.tool.OpsTools;

import java.time.Instant;

/**
 * 企业级智能运维排障 Agent 的最小演示入口。
 *
 * 这个 demo 故意使用规则模型和内存数据，让项目不依赖真实外部系统也能运行。
 * 后续接入 DeepSeek、Ollama 或 ChatGPT 时，只需要替换 DiagnosisModel 实现。
 */
public class EnterpriseOpsAgentDemo {

    public static void main(String[] args) {
        String providerName = args.length > 0
            ? args[0]
            : System.getProperty("ai.provider", System.getenv("AI_PROVIDER"));
        ModelSettings settings = ModelSettings.fromProviderName(providerName);
        System.out.println("model=" + settings.safeSummary());

        OpsTools tools = new OpsTools(
            InMemoryOpsServices.logs(),
            InMemoryOpsServices.metrics(),
            InMemoryOpsServices.deployments(),
            InMemoryOpsServices.approvals()
        );

        OpsAgentService agentService = new OpsAgentService(
            (serviceName, description) -> "如果支付回调超时，先检查最近发布、下游超时、线程池和网关错误率。",
            new OpsToolExecutor(tools),
            new RuleBasedDiagnosisModel(),
            new ConsoleAuditLogService()
        );

        OpsAlert alert = new OpsAlert(
            "ALERT-20260611-001",
            "payment-service",
            "prod",
            "P1",
            "支付服务错误率升高",
            "支付回调接口出现大量 timeout，P95 延迟明显升高。",
            Instant.now()
        );

        DiagnosisReport report = agentService.diagnose(alert);

        System.out.println("summary=" + report.summary());
        System.out.println("rootCauseHypothesis=" + report.rootCauseHypothesis());
        System.out.println("recommendation=" + report.recommendation());
        System.out.println("requiresApproval=" + report.requiresApproval());
    }
}
