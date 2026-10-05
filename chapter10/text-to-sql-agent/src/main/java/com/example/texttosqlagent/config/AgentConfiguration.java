package com.example.texttosqlagent.config;

import com.example.texttosqlagent.agent.AnswerNarrator;
import com.example.texttosqlagent.agent.AuditLogService;
import com.example.texttosqlagent.agent.ConsoleAuditLogService;
import com.example.texttosqlagent.agent.DataAnalysisAgentService;
import com.example.texttosqlagent.agent.LangChain4jSqlGenerationModel;
import com.example.texttosqlagent.agent.RuleBasedSqlGenerationModel;
import com.example.texttosqlagent.agent.SqlGenerationModel;
import com.example.texttosqlagent.agent.TemplateAnswerNarrator;
import com.example.texttosqlagent.schema.MetricDictionary;
import com.example.texttosqlagent.schema.SchemaInspector;
import com.example.texttosqlagent.schema.SchemaLinkingService;
import com.example.texttosqlagent.schema.TableSchema;
import com.example.texttosqlagent.sql.ReadOnlySqlExecutor;
import com.example.texttosqlagent.sql.SqlSafetyGuard;
import java.util.HashSet;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 全部 Bean 的装配中心。
 *
 * 最关键的一条装配规则（也是本项目离线友好的原因）：
 * - text2sql.ai.offline=true，或选择了需要密钥的供应商但环境变量里没配 key
 *   → 装配 RuleBasedSqlGenerationModel（离线规则模型）；
 * - 否则 → 装配 LangChain4jSqlGenerationModel（真实模型路径）。
 * 启动时打一行 safeSummary 日志说明走了哪条路径，密钥不会出现在日志里。
 */
@Configuration
public class AgentConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AgentConfiguration.class);

    /**
     * Schema 读取器：表结构和中文注释都从数据库元数据里读，不手抄配置。
     *
     * DataSource 工厂返回前已完成初始化和只读授权，因此这里不会读到未初始化的库。
     */
    @Bean
    public SchemaInspector schemaInspector(DataSource dataSource) {
        return new SchemaInspector(dataSource);
    }

    /** 启动时读一次全量 schema，后续选表、拼 prompt、护栏白名单都复用这份 */
    @Bean
    public List<TableSchema> tableSchemas(SchemaInspector schemaInspector) {
        return schemaInspector.inspect();
    }

    @Bean
    public MetricDictionary metricDictionary() {
        return new MetricDictionary();
    }

    @Bean
    public SchemaLinkingService schemaLinkingService(
        List<TableSchema> tableSchemas, MetricDictionary metricDictionary) {
        return new SchemaLinkingService(tableSchemas, metricDictionary);
    }

    /** 护栏使用业务授权范围与真实元数据的交集，不把“能发现”当作“已授权” */
    @Bean
    public SqlSafetyGuard sqlSafetyGuard(
        List<TableSchema> tableSchemas,
        @Value("${text2sql.guard.max-rows:200}") int maxRows) {
        return new SqlSafetyGuard(maxRows, new HashSet<>(
            tableSchemas.stream().map(TableSchema::name).toList()));
    }

    @Bean
    public ReadOnlySqlExecutor readOnlySqlExecutor(
        DataSource dataSource,
        @Value("${text2sql.guard.max-rows:200}") int maxRows,
        @Value("${text2sql.executor.timeout-seconds:5}") int timeoutSeconds) {
        return new ReadOnlySqlExecutor(dataSource, maxRows, timeoutSeconds);
    }

    /** 模型配置：供应商由 text2sql.ai.provider 决定，默认 deepseek */
    @Bean
    public ModelSettings modelSettings(@Value("${text2sql.ai.provider:deepseek}") String provider) {
        return ModelSettings.fromProviderName(provider);
    }

    /**
     * SQL 生成模型装配：离线开关或缺密钥时落到规则模型，保证没网没 key 也能跑。
     */
    @Bean
    public SqlGenerationModel sqlGenerationModel(
        ModelSettings modelSettings,
        MetricDictionary metricDictionary,
        @Value("${text2sql.ai.offline:false}") boolean offline) {
        if (offline || !modelSettings.hasCredential()) {
            // 走离线规则模型：课堂演示、CI 测试、没配 key 的开发机都靠它兜底
            String reason = offline
                ? "text2sql.ai.offline=true 强制离线"
                : "目标模型 " + modelSettings.safeSummary() + " 未配置密钥";
            log.info("text2sql 使用离线规则模型（{}）", reason);
            return new RuleBasedSqlGenerationModel();
        }
        log.info("text2sql 使用真实模型路径：{}", modelSettings.safeSummary());
        return new LangChain4jSqlGenerationModel(modelSettings, metricDictionary);
    }

    @Bean
    public AnswerNarrator answerNarrator() {
        return new TemplateAnswerNarrator();
    }

    @Bean
    public AuditLogService auditLogService() {
        return new ConsoleAuditLogService();
    }

    @Bean
    public DataAnalysisAgentService dataAnalysisAgentService(
        SchemaLinkingService schemaLinkingService,
        SqlGenerationModel sqlGenerationModel,
        SqlSafetyGuard sqlSafetyGuard,
        ReadOnlySqlExecutor readOnlySqlExecutor,
        AnswerNarrator answerNarrator,
        AuditLogService auditLogService,
        ModelSettings modelSettings) {
        return new DataAnalysisAgentService(
            schemaLinkingService, sqlGenerationModel, sqlSafetyGuard,
            readOnlySqlExecutor, answerNarrator, auditLogService,
            modelSettings.safeSummary());
    }
}
