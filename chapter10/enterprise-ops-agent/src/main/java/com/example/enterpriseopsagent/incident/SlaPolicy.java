package com.example.enterpriseopsagent.incident;

import com.example.enterpriseopsagent.domain.OpsAlert;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * 简化版 SLA 策略。
 *
 * 真实项目一般会把这部分配置到数据库或配置中心，这里先放在 Java 代码里，方便看清规则。
 * 级别越高，留给值班人的响应时间越短。
 */
public class SlaPolicy {

    /**
     * 计算响应截止时间：收到告警的时间 + 该级别允许的响应时长。
     */
    public Instant responseDeadline(OpsAlert alert, Instant receivedAt) {
        return receivedAt.plus(responseWindow(alert.severity()));
    }

    private Duration responseWindow(String severity) {
        // 告警平台传来的级别大小写不一定统一，先转成大写再匹配。
        String normalizedSeverity = severity == null
            ? ""
            : severity.toUpperCase(Locale.ROOT);
        return switch (normalizedSeverity) {
            case "P0" -> Duration.ofMinutes(5);
            case "P1" -> Duration.ofMinutes(15);
            case "P2" -> Duration.ofMinutes(30);
            case "P3" -> Duration.ofHours(2);
            // 没有约定的级别按最宽松的 4 小时处理，避免因为脏数据把事件判成超时。
            default -> Duration.ofHours(4);
        };
    }
}
