package com.example.codingagent.service;

public class CodingAgentJobNotFoundException extends RuntimeException {

    public CodingAgentJobNotFoundException(String taskId) {
        super("Coding Agent 任务不存在：" + taskId);
    }
}
