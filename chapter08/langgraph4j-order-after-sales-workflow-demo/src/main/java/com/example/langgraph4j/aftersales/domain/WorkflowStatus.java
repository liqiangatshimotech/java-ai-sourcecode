package com.example.langgraph4j.aftersales.domain;

/**
 * API 层看到的工作流状态。WAITING_* 表示 checkpoint 已保存，可以稍后恢复。
 */
public enum WorkflowStatus {
    RUNNING,
    WAITING_FOR_INFORMATION,
    WAITING_FOR_APPROVAL,
    COMPLETED,
    FAILED
}
