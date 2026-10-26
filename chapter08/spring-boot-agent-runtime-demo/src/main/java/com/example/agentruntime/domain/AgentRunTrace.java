package com.example.agentruntime.domain;

import java.util.List;
import java.util.Map;
/** 保存一次运行的不可变快照；内存仓库可替换成业务数据库。 */
public record AgentRunTrace(String runId, String userId, String goal, String orderNo,
        String status, String finalAnswer, List<AgentStepRecord> steps, Map<String, String> facts) {
    public AgentRunTrace { steps = List.copyOf(steps); facts = Map.copyOf(facts); }
}
