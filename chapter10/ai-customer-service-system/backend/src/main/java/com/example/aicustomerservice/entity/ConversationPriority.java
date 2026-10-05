package com.example.aicustomerservice.entity;

/**
 * 会话优先级。企业客户、高价值订单和负面情绪可以提升优先级。
 */
public enum ConversationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}
