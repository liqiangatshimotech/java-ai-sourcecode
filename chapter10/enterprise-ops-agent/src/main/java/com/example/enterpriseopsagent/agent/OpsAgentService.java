package com.example.enterpriseopsagent.agent;

import com.example.enterpriseopsagent.domain.DiagnosisReport;
import com.example.enterpriseopsagent.domain.OpsAlert;

/**
 * 运维排障 Agent 的应用服务。
 *
 * 这里先展示主流程，不把所有细节塞进一个方法里。
 * RAG 检索、工具调用、模型生成和审计日志都通过接口隔离，方便后续替换实现。
 */
public class OpsAgentService {

    private final RunbookRetriever runbookRetriever;
    private final OpsToolExecutor toolExecutor;
    private final DiagnosisModel diagnosisModel;
    private final AuditLogService auditLogService;

    public OpsAgentService(
        RunbookRetriever runbookRetriever,
        OpsToolExecutor toolExecutor,
        DiagnosisModel diagnosisModel,
        AuditLogService auditLogService
    ) {
        this.runbookRetriever = runbookRetriever;
        this.toolExecutor = toolExecutor;
        this.diagnosisModel = diagnosisModel;
        this.auditLogService = auditLogService;
    }

    public DiagnosisReport diagnose(OpsAlert alert) {
        // 1. 先检索 Runbook 和历史故障案例，避免模型只靠通用知识猜原因。
        String runbookContext = runbookRetriever.retrieve(alert.serviceName(), alert.description());

        // 2. 调用只读工具拿证据，例如日志、指标、最近发布记录。
        String evidence = toolExecutor.collectReadOnlyEvidence(alert);

        // 3. 把告警、知识库上下文和工具证据交给模型，生成结构化排障报告。
        DiagnosisReport report = diagnosisModel.generateReport(alert, runbookContext, evidence);

        // 4. 报告生成后马上写审计日志，方便后续回放和评测。
        auditLogService.recordDiagnosis(alert, report);

        return report;
    }
}
