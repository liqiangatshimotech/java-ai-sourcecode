package com.example.aicustomerservice.entity;

/**
 * 消息可见范围。
 *
 * CUSTOMER 表示客户和客服双方可见；INTERNAL 只用于客服团队内部协作，不能发送给客户。
 */
public enum MessageVisibility {
    CUSTOMER,
    INTERNAL
}
