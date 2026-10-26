package com.example.agentruntime.planner;

import com.example.agentruntime.domain.AgentState;
/** 可替换为真实模型规划器，但仍须返回校验后的 AgentDecision。 */
public interface AgentPlanner { AgentDecision next(AgentState state); }
