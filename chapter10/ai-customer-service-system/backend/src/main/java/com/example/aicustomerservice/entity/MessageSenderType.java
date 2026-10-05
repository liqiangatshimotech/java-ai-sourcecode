package com.example.aicustomerservice.entity;

/**
 * 消息发送方类型，用来区分客户、人工客服、AI 草稿以及系统流转事件。
 */
public enum MessageSenderType {
    CUSTOMER,
    AGENT,
    AI,
    SYSTEM
}
