package com.example.aiproduction.observability;

/**
 * 审计日志接口。
 *
 * <p>工具调用、权限拒绝、高风险审批、模型降级这类事件都应该进入审计日志。课程里用内存实现，生产环境可以写数据库或日志平台。
 */
public interface AuditLogger {

    void record(TraceEvent event);
}
