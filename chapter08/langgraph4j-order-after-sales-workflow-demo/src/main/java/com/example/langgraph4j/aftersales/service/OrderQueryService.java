package com.example.langgraph4j.aftersales.service;

import com.example.langgraph4j.aftersales.domain.OrderSnapshot;

/** 查询订单快照的端口。真实项目可以在这里接订单中心 RPC。 */
public interface OrderQueryService {
    OrderSnapshot findRequired(String orderId);
}
