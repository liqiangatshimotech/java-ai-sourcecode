package com.example.agentruntime.business;

/** shippedHoursAgo 为已发货小时数，promiseHours 为承诺送达时限。 */
public record OrderSnapshot(String orderNo, String status, int shippedHoursAgo, int promiseHours) {}
