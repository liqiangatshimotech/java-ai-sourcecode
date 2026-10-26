package com.example.agentruntime.tool;

/** 写工具必须经 Runtime 授权，Planner 不能自行批准。 */
public enum ToolRisk { READ_ONLY, WRITE_REQUIRES_CONFIRMATION }
