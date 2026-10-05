package com.example.texttosqlagent.web;

import com.example.texttosqlagent.agent.AnalysisReport;
import com.example.texttosqlagent.agent.AnalysisStatus;
import com.example.texttosqlagent.sql.QueryResult;
import java.util.List;

/**
 * 提问响应体：从 AnalysisReport 转的 DTO，外加前 20 行数据预览。
 *
 * 审计报告里的耗时、请求 ID 等字段对排查问题有用，但对调用方不是必需的，
 * 这里只保留前端/调用方真正关心的内容。
 */
public record AskResponse(
    AnalysisStatus status,
    String sql,
    String summary,
    List<String> linkedTables,
    List<String> violations,
    Integer rowCount,
    long durationMs,
    List<List<Object>> rows
) {

    /** 数据预览最多返回的行数，防止大结果集撑爆响应体 */
    private static final int PREVIEW_LIMIT = 20;

    /**
     * @param report 编排层返回的审计报告
     * @param result 查询结果集（未执行到查询的请求为 null）
     */
    public static AskResponse from(AnalysisReport report, QueryResult result) {
        List<List<Object>> preview = result == null
            ? List.of()
            : result.rows().stream().limit(PREVIEW_LIMIT).toList();
        return new AskResponse(
            report.status(),
            report.generatedSql(),
            report.summary(),
            report.linkedTables(),
            report.violations(),
            report.rowCount(),
            report.durationMs(),
            preview
        );
    }
}
