package com.example.enterpriseopsagent.agent;

/**
 * Runbook 检索接口。
 *
 * 当前 demo 用字符串模拟 RAG 检索结果。真实项目里，这里会连接向量库，
 * 按服务名、错误码和告警描述召回运维手册与历史故障案例。
 */
public interface RunbookRetriever {
    String retrieve(String serviceName, String alertDescription);
}
