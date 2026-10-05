package com.example.texttosqlagent.agent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 一次数据分析请求的完整审计报告。
 *
 * 企业场景里「AI 当时为什么给出这个数」必须可复盘，
 * 所以把整条链路的中间产物都留在报告里：
 * 选了哪些表、生成了什么 SQL、护栏有没有拦、查了几行、模型是谁、耗时多少。
 *
 * @param requestId    请求唯一 ID（UUID 前 8 位，够审计检索用）
 * @param question     用户原始问题
 * @param linkedTables Schema Linking 选中的表名
 * @param generatedSql 模型生成的 SQL（护栏拦截时也保留，便于复盘）
 * @param violations   护栏违规明细
 * @param rowCount     结果行数；未执行到查询时为 null
 * @param summary      给用户的最终中文结论（含拒答/拦截原因说明）
 * @param modelSummary 模型配置的安全摘要（不含密钥）
 * @param status       最终状态
 * @param durationMs   全链路耗时（毫秒）
 * @param createdAt    请求发生时间
 */
public record AnalysisReport(
    String requestId,
    String question,
    List<String> linkedTables,
    String generatedSql,
    List<String> violations,
    Integer rowCount,
    String summary,
    String modelSummary,
    AnalysisStatus status,
    long durationMs,
    LocalDateTime createdAt
) {
}
