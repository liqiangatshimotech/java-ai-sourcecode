package com.example.aiproduction.service;

/**
 * 上线前检查请求。
 *
 * <p>prompt 代表用户或业务系统发给 Agent 的输入，taskType 用来做模型路由，requestedTool 表示 Agent 准备调用的工具。
 */
public record ProductionReadinessRequest(
        String traceId, String taskType, String prompt, String requestedTool, int expectedOutputTokens) {}
