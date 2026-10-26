package com.example.agentruntime.business;

/** hoursSinceLastUpdate 衡量物流停滞时间，不等同于超出承诺的小时数。 */
public record LogisticsSnapshot(String orderNo, int hoursSinceLastUpdate) {}
