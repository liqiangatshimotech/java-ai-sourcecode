package com.example.enterpriseopsagent.agent;

import com.example.enterpriseopsagent.domain.OpsAlert;

import java.util.List;
import java.util.Locale;

/**
 * 审批判定规则：一份处理建议要不要先过人工审批。
 *
 * 这件事不能让模型自己说了算，所以规则写在 Java 里，
 * 规则模型和真实模型生成报告时调用的是同一个方法。命中任意一条就需要审批：
 * 1. 处理建议里出现回滚、重启、扩缩容、切流、限流这类会改动线上的动作；
 * 2. 告警来自生产环境，并且级别是 P0 或 P1。
 * 两条都不命中时不创建审批单，事件停在 DIAGNOSED，由值班人按建议跟进。
 */
public final class ApprovalPolicy {

    /**
     * 会改动线上状态的动作关键词。
     * 这里用最朴素的包含匹配：宁可多拦一次审批，也不要漏掉一次变更。
     */
    private static final List<String> CHANGE_ACTION_KEYWORDS = List.of(
        "回滚", "重启", "扩容", "缩容", "切流", "限流", "rollback", "restart", "scale"
    );

    private ApprovalPolicy() {
    }

    /**
     * @param alert          触发诊断的告警，用来判断环境和级别
     * @param recommendation 报告里的处理建议，可能来自规则模型，也可能来自真实模型
     * @return true 表示必须先创建审批单，等人工决策
     */
    public static boolean requiresApproval(OpsAlert alert, String recommendation) {
        return mentionsChangeAction(recommendation) || isUrgentProductionAlert(alert);
    }

    /**
     * 生产环境的 P0 / P1 告警影响面大，不管建议写了什么，都先交给人确认。
     */
    public static boolean isUrgentProductionAlert(OpsAlert alert) {
        String severity = alert.severity() == null
            ? ""
            : alert.severity().trim().toUpperCase(Locale.ROOT);
        return "prod".equalsIgnoreCase(alert.environment().trim())
            && (severity.equals("P0") || severity.equals("P1"));
    }

    private static boolean mentionsChangeAction(String recommendation) {
        if (recommendation == null) {
            return false;
        }
        String text = recommendation.toLowerCase(Locale.ROOT);
        return CHANGE_ACTION_KEYWORDS.stream().anyMatch(text::contains);
    }
}
