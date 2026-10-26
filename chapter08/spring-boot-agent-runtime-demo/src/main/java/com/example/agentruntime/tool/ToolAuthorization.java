package com.example.agentruntime.tool;

/** 权限检查返回结构化结果，让 Runtime 能进入 WAITING_APPROVAL。 */
public record ToolAuthorization(boolean allowed, String reason) {
    public static ToolAuthorization permitted() { return new ToolAuthorization(true, ""); }
    public static ToolAuthorization blocked(String reason) { return new ToolAuthorization(false, reason); }
}
