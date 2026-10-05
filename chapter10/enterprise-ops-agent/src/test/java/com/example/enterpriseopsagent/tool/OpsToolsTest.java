package com.example.enterpriseopsagent.tool;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpsToolsTest {

    @Test
    void shouldLimitLogQueryWindowToSixtyMinutes() {
        AtomicInteger capturedMinutes = new AtomicInteger();
        OpsTools tools = new OpsTools(
            (serviceName, environment, keyword, minutes) -> {
                capturedMinutes.set(minutes);
                return "logs";
            },
            (serviceName, environment, minutes) -> "metrics",
            (serviceName, environment) -> "deployments",
            (serviceName, environment, reason) -> "approval"
        );

        tools.queryLogs("payment-service", "prod", "timeout", 120);

        assertEquals(60, capturedMinutes.get());
    }

    @Test
    void shouldRaiseMetricQueryWindowToAtLeastOneMinute() {
        AtomicInteger capturedMinutes = new AtomicInteger();
        OpsTools tools = new OpsTools(
            (serviceName, environment, keyword, minutes) -> "logs",
            (serviceName, environment, minutes) -> {
                capturedMinutes.set(minutes);
                return "metrics";
            },
            (serviceName, environment) -> "deployments",
            (serviceName, environment, reason) -> "approval"
        );

        tools.queryMetrics("payment-service", "prod", -5);

        assertEquals(1, capturedMinutes.get());
    }

    @Test
    void shouldCreateApprovalRequestInsteadOfExecutingRollback() {
        OpsTools tools = new OpsTools(
            (serviceName, environment, keyword, minutes) -> "logs",
            (serviceName, environment, minutes) -> "metrics",
            (serviceName, environment) -> "deployments",
            (serviceName, environment, reason) -> "approval-created:" + reason
        );

        String result = tools.requestRollbackApproval("payment-service", "prod", "发布后错误率升高");

        assertTrue(result.contains("approval-created"));
    }
}
