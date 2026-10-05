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
        String recommendation = "先暂停继续发布，提交回滚审批；审批通过后回滚到上一稳定版本，并继续观察错误率。";

        return new DiagnosisReport(
            alert.alertId(),
            summary,
            hypothesis,
            "Runbook：" + runbookContext + "\n\n工具证据：\n" + evidence,
            recommendation,
            true
        );
    }
}
