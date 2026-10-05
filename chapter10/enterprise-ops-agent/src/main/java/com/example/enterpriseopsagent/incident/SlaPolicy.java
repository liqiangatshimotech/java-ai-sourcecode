package com.example.enterpriseopsagent.incident;

import com.example.enterpriseopsagent.domain.OpsAlert;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * 简化版 SLA 策略。
 *
 * 真实项目一般会把这部分配置到数据库或配置中心，这里先放在 Java 代码里，方便看清规则。
 */
public class SlaPolicy {

    public Instant responseDeadline(OpsAlert alert, Instant receivedAt) {
        return receivedAt.plus(responseWindow(alert.severity()));
    }

    private Duration responseWindow(String severity) {
        String normalizedSeverity = severity == null
            ? ""
            : severity.toUpperCase(Locale.ROOT);
        return switch (normalizedSeverity) {
            case "P1" -> Duration.ofMinutes(15);
            case "P2" -> Duration.ofMinutes(30);
            case "P3" -> Duration.ofHours(2);
            default -> Duration.ofHours(4);
        };
    }
}
