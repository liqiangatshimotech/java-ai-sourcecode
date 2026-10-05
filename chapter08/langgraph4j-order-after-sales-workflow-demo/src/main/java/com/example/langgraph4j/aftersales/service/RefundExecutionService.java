package com.example.langgraph4j.aftersales.service;

import com.example.langgraph4j.aftersales.domain.OrderSnapshot;
import com.example.langgraph4j.aftersales.domain.RefundReceipt;

/** 执行退款的端口。idempotencyKey 必须由工作流 threadId 稳定生成。 */
public interface RefundExecutionService {
    RefundReceipt refund(String idempotencyKey, OrderSnapshot order);
}
