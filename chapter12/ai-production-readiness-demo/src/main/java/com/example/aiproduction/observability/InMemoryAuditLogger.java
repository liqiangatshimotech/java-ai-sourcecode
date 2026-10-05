package com.example.aiproduction.observability;

import java.util.ArrayList;
import java.util.List;

/**
 * 内存审计日志。
 *
 * <p>它适合单元测试和命令行演示。真实服务不要只存在内存里，否则进程重启后审计记录会丢失。
 */
public class InMemoryAuditLogger implements AuditLogger {

    private final List<TraceEvent> records = new ArrayList<>();

    @Override
    public void record(TraceEvent event) {
        records.add(event);
    }

    public List<TraceEvent> records() {
        return List.copyOf(records);
    }
}
