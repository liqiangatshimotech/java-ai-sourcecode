package com.example.texttosqlagent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 控制台审计日志实现：一行结构化日志记录一次分析请求的关键信息。
 *
 * 刻意只打 safeSummary（供应商/模型名/端点），密钥永远不进日志。
 */
public class ConsoleAuditLogService implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleAuditLogService.class);

    @Override
    public void record(AnalysisReport report) {
        log.info(
            "text2sql审计 requestId={} status={} 行数={} 耗时={}ms 模型={} 问题={} SQL={} 违规={}",
            report.requestId(),
            report.status(),
            report.rowCount(),
            report.durationMs(),
            report.modelSummary(),
            report.question(),
            report.generatedSql(),
            report.violations()
        );
    }
}
