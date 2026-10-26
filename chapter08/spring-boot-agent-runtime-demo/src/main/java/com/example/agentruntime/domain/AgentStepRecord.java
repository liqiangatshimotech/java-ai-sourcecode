package com.example.agentruntime.domain;

import java.time.Instant;
import java.util.Map;
/** thought 仅记录可公开的动作理由，不保存模型隐藏推理。参数使用不可变快照。 */
public record AgentStepRecord(int stepNo, String thought, String toolName,
        Map<String, String> toolArgs, String status, String observation, Instant createdAt) {
    public AgentStepRecord { toolArgs = Map.copyOf(toolArgs); }
    public String toLogLine() {
        return "step=" + stepNo + ", tool=" + toolName + ", status=" + status
                + ", thought=" + thought + ", observation=" + observation;
    }
}
