package com.example.enterpriseopsagent.agent;

import com.example.enterpriseopsagent.domain.DiagnosisReport;
import com.example.enterpriseopsagent.domain.OpsAlert;

/**
 * Demo 版排障模型。
 *
 * 这里不用真实大模型，而是用规则生成报告，保证示例可以离线运行。
 * 接入 DeepSeek、Ollama 或 ChatGPT 时，只需要替换 DiagnosisModel 的实现。
 */
public class RuleBasedDiagnosisModel implements DiagnosisModel {

    @Override
    public DiagnosisReport generateReport(OpsAlert alert, String runbookContext, String evidence) {
        String summary = alert.serviceName() + " 在 " + alert.environment() + " 环境出现高错误率和延迟升高。";
        String hypothesis = "故障高度疑似与最近一次发布有关，支付回调超时时间调整后触发下游超时。";
        // 生产环境的 P0 / P1 直接建议走回滚审批；级别低或非生产环境先按 Runbook 排查，不动线上。
        String recommendation = ApprovalPolicy.isUrgentProductionAlert(alert)
            ? "先暂停继续发布，提交回滚审批；审批通过后回滚到上一稳定版本，并继续观察错误率。"
            : "暂不需要变更操作：由服务负责人按 Runbook 核对最近发布和下游超时，继续观察错误率和延迟。";

        return new DiagnosisReport(
            alert.alertId(),
            summary,
            hypothesis,
            "Runbook：" + runbookContext + "\n\n工具证据：\n" + evidence,
            recommendation,
            // 要不要审批不在这里写死，统一交给 ApprovalPolicy 判定。
            ApprovalPolicy.requiresApproval(alert, recommendation)
        );
    }
}
