package com.example.enterpriseopsagent.incident;

import com.example.enterpriseopsagent.domain.IncidentStatus;

/**
 * 排障事件查询条件。
 *
 * Web 层把 URL query 参数转成这个对象，仓储层只关心业务筛选条件。
 */
public record IncidentQuery(
    IncidentStatus status,
    String serviceName,
    String environment
) {
}
