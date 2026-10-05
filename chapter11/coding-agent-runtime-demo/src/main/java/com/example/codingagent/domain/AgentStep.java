package com.example.codingagent.domain;

import com.example.codingagent.tool.ToolResult;

import java.time.Instant;

/**
 * Agent 每执行一个动作都落一条 Step。
 * 真实系统里这类数据通常会进入数据库，便于审计、回放和故障定位。
 */
public record AgentStep(
        int index,
        String name,
        String toolName,
        Instant startedAt,
        Instant endedAt,
        ToolResult result
) {
}
