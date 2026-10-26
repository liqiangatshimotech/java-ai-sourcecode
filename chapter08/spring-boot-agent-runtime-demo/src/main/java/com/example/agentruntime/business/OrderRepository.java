package com.example.agentruntime.business;

/** 替换订单数据库时只替换这个端口，不改变 Runtime。 */
public interface OrderRepository { OrderSnapshot findByOrderNo(String orderNo); }
