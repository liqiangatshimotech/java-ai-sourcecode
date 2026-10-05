package com.example.aiproduction.observability;

import java.time.Instant;
import java.util.Map;

/**
 * Agent 执行过程中的一条可观测事件。
 *
 * <p>metadata 用来放模型名、工具名、Token 用量、错误码等扩展字段。真实项目里可以把它写入日志平台、APM 或数据仓库。
 */
public record TraceEvent(String traceId, String type, String message, Map<String, String> metadata, Instant at) {}
