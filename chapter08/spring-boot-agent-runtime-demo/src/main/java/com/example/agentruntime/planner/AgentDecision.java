package com.example.agentruntime.planner;

import com.example.agentruntime.tool.ToolCall;
/** 规划器仅能建议动作或结束，不能直接执行业务写操作。 */
public record AgentDecision(boolean finished, String thought, ToolCall call, String answer) {
    public AgentDecision {
        if (finished ? call != null || answer == null || answer.isBlank() : call == null || answer != null)
            throw new IllegalArgumentException("决策必须且只能包含工具调用或最终答案");
    }
    public static AgentDecision callTool(String thought, ToolCall call) { return new AgentDecision(false, thought, call, null); }
    public static AgentDecision finish(String answer) { return new AgentDecision(true, "", null, answer); }
}
