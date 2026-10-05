package com.example.texttosqlagent.agent;

/**
 * SQL 生成模型抽象：给定问题和 schema 上下文，产出一条候选 SQL。
 *
 * 有两个实现：
 * - LangChain4jSqlGenerationModel：真实模型路径（DeepSeek / Ollama / ChatGPT）；
 * - RuleBasedSqlGenerationModel：离线规则替身，没网没 key 时兜底，也是测试的固定桩。
 *
 * 约定：模型答不上来时必须返回常量 CANNOT_ANSWER，而不是硬编一条 SQL——
 * 宁可拒答，也不让幻觉 SQL 进入执行链路。
 */
public interface SqlGenerationModel {

    /** 模型无法回答时的统一返回值 */
    String CANNOT_ANSWER = "CANNOT_ANSWER";

    /**
     * @param question      用户的自然语言问题
     * @param schemaContext Schema Linking 拼好的 DDL + 指标口径上下文
     * @return 一条候选 SQL，或 CANNOT_ANSWER
     */
    String generateSql(String question, String schemaContext);
}
