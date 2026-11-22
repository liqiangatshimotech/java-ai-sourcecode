package com.example.enterpriseopsagent.agent;

import com.example.enterpriseopsagent.domain.OpsAlert;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审批判定规则的单元测试：需要审批和不需要审批两条分支都要覆盖。
 */
class ApprovalPolicyTest {

    private static final String OBSERVE_ONLY = "由服务负责人按 Runbook 排查，继续观察错误率和延迟。";

    @Test
    void shouldRequireApprovalWhenRecommendationMentionsChangeAction() {
        // 即使是预发环境的低级别告警，只要建议里出现变更动作，就要先审批。
        OpsAlert alert = alert("staging", "P3");

        assertTrue(ApprovalPolicy.requiresApproval(alert, "建议回滚到上一稳定版本。"));
        assertTrue(ApprovalPolicy.requiresApproval(alert, "建议重启实例后观察。"));
        assertTrue(ApprovalPolicy.requiresApproval(alert, "Rollback to the previous release."));
    }

    @Test
    void shouldRequireApprovalForUrgentProductionAlert() {
        // 生产环境的 P0 / P1，建议里没有变更动作也要人工确认。
        assertTrue(ApprovalPolicy.requiresApproval(alert("prod", "P0"), OBSERVE_ONLY));
        assertTrue(ApprovalPolicy.requiresApproval(alert("prod", "p1"), OBSERVE_ONLY));
    }

    @Test
    void shouldNotRequireApprovalForLowRiskAlert() {
        // 两条规则都不命中：不创建审批单，事件会停在 DIAGNOSED。
        assertFalse(ApprovalPolicy.requiresApproval(alert("prod", "P3"), OBSERVE_ONLY));
        assertFalse(ApprovalPolicy.requiresApproval(alert("staging", "P1"), OBSERVE_ONLY));
        assertFalse(ApprovalPolicy.requiresApproval(alert("staging", "P3"), null));
    }

    private OpsAlert alert(String environment, String severity) {
        return new OpsAlert(
            "ALERT-APPROVAL-001",
            "payment-service",
            environment,
            severity,
            "支付服务错误率升高",
            "支付回调接口出现 timeout。",
            Instant.now()
        );
    }
}
