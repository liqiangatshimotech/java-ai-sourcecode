package com.example.agentscopeframework.project;

/**
 * 工单优先级。
 *
 * <p>真实系统里通常会把它映射到数据库枚举、SLA 策略和告警升级策略。
 */
public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}
