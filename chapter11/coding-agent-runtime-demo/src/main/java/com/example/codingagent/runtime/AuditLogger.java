package com.example.codingagent.runtime;

import com.example.codingagent.domain.AgentSession;

public interface AuditLogger {

    void log(AgentSession session, String event, String detail);
}
