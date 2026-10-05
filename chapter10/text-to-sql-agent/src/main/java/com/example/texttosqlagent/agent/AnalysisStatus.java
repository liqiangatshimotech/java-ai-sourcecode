package com.example.texttosqlagent.agent;

/**
 * 一次数据分析请求的最终状态。
 *
 * 状态机刻意区分「答不上」和「不允许答」：
 * - NO_MATCHING_TABLE / CANNOT_ANSWER：能力边界，诚实拒答；
 * - BLOCKED_BY_GUARD：安全边界，SQL 被护栏拦下，根本没有执行；
 * - EXECUTION_FAILED：SQL 本身合法但执行出错（比如模型写了不存在的列）。
 */
public enum AnalysisStatus {
    /** 正常完成：查到结果并生成了中文解说 */
    ANSWERED,
    /** SQL 被安全护栏拦截，未执行 */
    BLOCKED_BY_GUARD,
    /** Schema Linking 没找到相关表 */
    NO_MATCHING_TABLE,
    /** 模型（或规则）无法为这个问题生成 SQL */
    CANNOT_ANSWER,
    /** SQL 执行阶段出错 */
    EXECUTION_FAILED
}
