package com.example.codingagent.tool;

public record ToolResult(
        boolean success,
        String summary,
        String output
) {

    public static ToolResult success(String summary, String output) {
        return new ToolResult(true, summary, output);
    }

    public static ToolResult failure(String summary, String output) {
        return new ToolResult(false, summary, output);
    }
}
