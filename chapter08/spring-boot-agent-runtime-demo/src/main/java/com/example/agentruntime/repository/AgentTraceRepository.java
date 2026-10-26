package com.example.agentruntime.repository;

import com.example.agentruntime.domain.AgentRunTrace;
import java.util.Optional;
/** 保存每步后的不可变运行快照，而不只在最终响应时保存。 */
public interface AgentTraceRepository {
    void save(AgentRunTrace trace);
    Optional<AgentRunTrace> findByRunId(String runId);
}
