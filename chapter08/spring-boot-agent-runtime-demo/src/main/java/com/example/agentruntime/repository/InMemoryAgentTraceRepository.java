package com.example.agentruntime.repository;

import com.example.agentruntime.domain.AgentRunTrace;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
/** 仅当前 JVM 有效；没有宣称重启后恢复。 */
public final class InMemoryAgentTraceRepository implements AgentTraceRepository {
    private final ConcurrentHashMap<String, AgentRunTrace> traces = new ConcurrentHashMap<>();
    public void save(AgentRunTrace trace) { traces.put(trace.runId(), trace); }
    public Optional<AgentRunTrace> findByRunId(String runId) { return Optional.ofNullable(traces.get(runId)); }
}
