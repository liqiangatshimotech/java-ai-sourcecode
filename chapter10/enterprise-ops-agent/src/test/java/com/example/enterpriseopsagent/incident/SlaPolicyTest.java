package com.example.enterpriseopsagent.incident;

import com.example.enterpriseopsagent.domain.OpsAlert;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SLA 规则的单元测试：级别越高，响应截止时间越早。
 */
class SlaPolicyTest {

    private final SlaPolicy slaPolicy = new SlaPolicy();
    private final Instant receivedAt = Instant.parse("2026-06-11T07:00:00Z");

    @Test
    void shouldGiveP0TighterDeadlineThanP1() {
        Instant p0Deadline = slaPolicy.responseDeadline(alert("P0"), receivedAt);
        Instant p1Deadline = slaPolicy.responseDeadline(alert("P1"), receivedAt);

        assertEquals(Instant.parse("2026-06-11T07:05:00Z"), p0Deadline);
        assertEquals(Instant.parse("2026-06-11T07:15:00Z"), p1Deadline);
        assertTrue(p0Deadline.isBefore(p1Deadline));
    }

    @Test
    void shouldUseFourHoursForUnknownSeverity() {
        // 告警平台传来没约定过的级别时，按最宽松的 4 小时处理。
        assertEquals(
            Instant.parse("2026-06-11T11:00:00Z"),
            slaPolicy.responseDeadline(alert("UNKNOWN"), receivedAt)
        );
    }

    private OpsAlert alert(String severity) {
        return new OpsAlert(
            "ALERT-SLA-001",
            "payment-service",
            "prod",
            severity,
            "支付服务错误率升高",
            "支付回调接口出现 timeout。",
            receivedAt
        );
    }
}
