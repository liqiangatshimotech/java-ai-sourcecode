package com.example.codingagent.runtime;

import com.example.codingagent.domain.AgentSession;

/**
 * 简单控制台审计日志。
 * 生产环境里建议写入数据库或日志平台，并补充 traceId、requestId 和操作者信息。
 */
public final class ConsoleAuditLogger implements AuditLogger {

    @Override
    public void log(AgentSession session, String event, String detail) {
        System.out.printf("[audit] session=%s event=%s detail=%s%n",
                session.sessionId(), event, detail);
    }
}
