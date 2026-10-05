package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.ToolTrace;

public record ToolTraceResponse(
    String toolName,
    String summary
) {
    public static ToolTraceResponse from(ToolTrace trace) {
        return new ToolTraceResponse(trace.toolName(), trace.summary());
    }
}
