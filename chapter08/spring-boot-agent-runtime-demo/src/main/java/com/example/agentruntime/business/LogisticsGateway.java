package com.example.agentruntime.business;

/** 物流接口端口，可在测试中注入边界值或异常。 */
public interface LogisticsGateway { LogisticsSnapshot query(String orderNo); }
