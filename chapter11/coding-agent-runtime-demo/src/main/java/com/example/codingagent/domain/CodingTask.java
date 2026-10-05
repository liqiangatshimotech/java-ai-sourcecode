package com.example.codingagent.domain;

/**
 * 从用户或业务系统传入的 Coding Agent 任务。
 */
public record CodingTask(
        String taskId,
        String description,
        String operator
) {
}
