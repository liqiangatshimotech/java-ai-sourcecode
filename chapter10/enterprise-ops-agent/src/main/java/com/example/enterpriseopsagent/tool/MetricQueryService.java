package com.example.enterpriseopsagent.tool;

/**
 * 指标查询服务。
 *
 * 这里只返回文本，方便最小 demo 跑通。真实项目里可以返回结构化指标，
 * 例如错误率、P95 延迟、CPU、内存、实例数和流量。
 */
public interface MetricQueryService {
    String queryServiceMetrics(String serviceName, String environment, int minutes);
}
