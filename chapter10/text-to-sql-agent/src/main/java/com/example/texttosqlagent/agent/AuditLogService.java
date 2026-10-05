package com.example.texttosqlagent.agent;

/**
 * 审计日志：每一次分析请求（无论成功、拒答还是被拦截）都必须留痕。
 *
 * 接口化是为了让存储可替换：教学项目打控制台日志，
 * 企业生产通常落库或发到日志平台，编排层不需要改。
 */
public interface AuditLogService {

    /** 记录一份完整的分析报告 */
    void record(AnalysisReport report);
}
