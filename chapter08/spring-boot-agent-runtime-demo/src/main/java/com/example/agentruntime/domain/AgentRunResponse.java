package com.example.agentruntime.domain;

import java.util.List;
import java.util.Map;
/** finalAnswer 描述已完成的事实，pendingApproval 为空表示当前没有挂起动作。 */
public record AgentRunResponse(String runId, String userId, String provider, String status,
        String finalAnswer, String pendingApproval, List<AgentStepRecord> steps,
        Map<String, String> facts) {}
