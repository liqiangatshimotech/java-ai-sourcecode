package com.example.enterpriseopsagent.tool;

/**
 * 日志查询服务。
 *
 * Demo 里用接口隔离真实日志平台，后面可以替换成 Elasticsearch、Loki、
 * 云厂商日志服务，或者通过 MCP Server 暴露出来的日志工具。
 */
public interface LogQueryService {
    String searchErrorLogs(String serviceName, String environment, String keyword, int minutes);
}
