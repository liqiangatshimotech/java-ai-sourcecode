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
