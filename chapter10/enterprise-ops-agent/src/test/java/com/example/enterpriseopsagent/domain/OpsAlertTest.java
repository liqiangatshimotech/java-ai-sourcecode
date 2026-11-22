package com.example.enterpriseopsagent.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpsAlertTest {

    @Test
    void shouldRejectBlankAlertId() {
        assertThrows(IllegalArgumentException.class, () -> new OpsAlert(
            " ",
            "payment-service",
            "prod",
            "P1",
            "支付服务错误率升高",
            "支付回调接口出现 timeout。",
            Instant.now()
        ));
    }

    @Test
    void shouldRejectBlankServiceName() {
        // 没有服务名就没法查日志、指标和发布记录，直接拒绝。
        assertThrows(IllegalArgumentException.class, () -> new OpsAlert(
            "ALERT-001",
            "",
            "prod",
            "P1",
            "支付服务错误率升高",
            "支付回调接口出现 timeout。",
            Instant.now()
        ));
    }

    @Test
    void shouldRejectBlankEnvironment() {
        // 环境决定审批策略，不能缺。
        assertThrows(IllegalArgumentException.class, () -> new OpsAlert(
            "ALERT-001",
            "payment-service",
            " ",
            "P1",
            "支付服务错误率升高",
            "支付回调接口出现 timeout。",
            Instant.now()
        ));
    }

    @Test
    void shouldFillTriggeredAtWhenInputIsNull() {
        OpsAlert alert = new OpsAlert(
            "ALERT-001",
            "payment-service",
            "prod",
            "P1",
            "支付服务错误率升高",
            "支付回调接口出现 timeout。",
            null
        );

        assertNotNull(alert.triggeredAt());
    }
}
