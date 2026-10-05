package com.example.enterpriseopsagent.tool;

/**
 * 发布记录查询服务。
 *
 * 排障时要先判断故障是否和近期变更有关，所以发布记录是运维 Agent 的核心证据。
 */
public interface DeploymentService {
    String findRecentDeployments(String serviceName, String environment);
}
