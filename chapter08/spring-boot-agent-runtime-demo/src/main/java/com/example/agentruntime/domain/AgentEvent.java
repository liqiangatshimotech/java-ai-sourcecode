package com.example.agentruntime.domain;

import java.time.Instant;
/** 事件属于通知通道；sequence 是单次运行内单调递增的回放编号。 */
public record AgentEvent(long sequence, String runId, String type, String message, Instant createdAt) {}
