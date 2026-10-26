package com.example.agentruntime.service;

import com.example.agentruntime.domain.AgentState;
/** maxSteps 限制工具尝试次数（包括 BLOCKED/FAILED），最终 Finish 不消耗工具额度。 */
public record StopCondition(int maxSteps) {
    public StopCondition { if (maxSteps < 1) throw new IllegalArgumentException("maxSteps 必须大于 0"); }
    public boolean shouldStop(AgentState state) { return state.nextStepNo() > maxSteps; }
}
