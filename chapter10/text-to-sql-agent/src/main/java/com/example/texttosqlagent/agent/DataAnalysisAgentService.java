package com.example.texttosqlagent.agent;

import com.example.texttosqlagent.schema.SchemaLinkingResult;
import com.example.texttosqlagent.schema.SchemaLinkingService;
import com.example.texttosqlagent.schema.TableSchema;
import com.example.texttosqlagent.sql.QueryResult;
import com.example.texttosqlagent.sql.ReadOnlySqlExecutor;
import com.example.texttosqlagent.sql.SqlGuardResult;
import com.example.texttosqlagent.sql.SqlSafetyGuard;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 数据分析 Agent 的编排入口：把各个环节串成一条完整链路。
 *
 * 流程（每一步失败都有对应状态，绝不把问题糊弄过去）：
 * 1. Schema Linking 选表——一张都没选中就诚实回复「找不到相关表」（NO_MATCHING_TABLE）；
 * 2. 模型生成 SQL——模型答不上就诚实拒答（CANNOT_ANSWER），宁可拒答不编 SQL；
 * 3. 安全护栏校验——违规直接拦下（BLOCKED_BY_GUARD），SQL 不会进入执行器；
 * 4. 只读执行——异常转成 EXECUTION_FAILED，回给调用方的是脱敏说明，不抛原始栈；
 * 5. 结果解说——把结果集翻译成中文结论；
 * 6. 审计留痕——无论成败都写审计日志后再返回。
 */
public class DataAnalysisAgentService {

    private final SchemaLinkingService linkingService;
    private final SqlGenerationModel model;
    private final SqlSafetyGuard guard;
    private final ReadOnlySqlExecutor executor;
    private final AnswerNarrator narrator;
    private final AuditLogService auditLog;
    private final String modelSummary;

    public DataAnalysisAgentService(
        SchemaLinkingService linkingService,
        SqlGenerationModel model,
        SqlSafetyGuard guard,
        ReadOnlySqlExecutor executor,
        AnswerNarrator narrator,
        AuditLogService auditLog,
        String modelSummary
    ) {
        this.linkingService = linkingService;
        this.model = model;
        this.guard = guard;
        this.executor = executor;
        this.narrator = narrator;
        this.auditLog = auditLog;
        this.modelSummary = modelSummary;
    }

    /**
     * 分析一个自然语言问题，返回审计报告（常规入口，Web 层以外都够用）。
     */
    public AnalysisReport ask(String question) {
        return askWithResult(question).report();
    }

    /**
     * 分析并同时返回查询结果集（Web 层需要把前 20 行预览放进响应体）。
     */
    public AnalysisOutcome askWithResult(String question) {
        long startNanos = System.nanoTime();
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        // 第 1 步：Schema Linking 选表
        SchemaLinkingResult linking = linkingService.link(question);
        List<String> linkedTables = linking.selectedTables().stream().map(TableSchema::name).toList();
        if (linking.selectedTables().isEmpty()) {
            // 诚实回复找不到相关表，而不是硬编一条 SQL 去碰运气
            return finish(requestId, question, linkedTables, null, List.of(), null,
                "在数据库里找不到与这个问题相关的表，请换个说法，比如问问销售额、订单、客户或支付相关的问题。",
                AnalysisStatus.NO_MATCHING_TABLE, startNanos, null);
        }

        // 第 2 步：模型生成 SQL
        String generatedSql = model.generateSql(question, linking.schemaContext());
        if (generatedSql == null || generatedSql.isBlank()
            || SqlGenerationModel.CANNOT_ANSWER.equals(generatedSql.trim())) {
            // 幻觉兜底：模型答不上就直说，绝不允许编 SQL
            return finish(requestId, question, linkedTables, null, List.of(), null,
                "这个问题超出了我目前能回答的范围，暂时无法生成可靠的查询，请换一种问法试试。",
                AnalysisStatus.CANNOT_ANSWER, startNanos, null);
        }

        // 第 3 步：安全护栏校验，违规 SQL 到此为止，不会进入执行器
        SqlGuardResult guardResult = guard.check(generatedSql);
        if (!guardResult.passed()) {
            return finish(requestId, question, linkedTables, generatedSql, guardResult.violations(), null,
                "生成的 SQL 未通过安全校验，已拦截不予执行。违规明细：" + String.join("；", guardResult.violations()),
                AnalysisStatus.BLOCKED_BY_GUARD, startNanos, null);
        }

        // 第 4 步：只读执行；执行异常不抛栈给调用方，转成脱敏说明
        QueryResult result;
        try {
            result = executor.execute(guardResult.sanitizedSql());
        } catch (SQLException e) {
            return finish(requestId, question, linkedTables, guardResult.sanitizedSql(), List.of(), null,
                "查询执行失败：SQL 可能引用了不存在的字段或表，请换个说法重试。",
                AnalysisStatus.EXECUTION_FAILED, startNanos, null);
        }

        // 第 5 步：把结果集翻译成中文结论
        String summary = narrator.narrate(question, result);
        return finish(requestId, question, linkedTables, guardResult.sanitizedSql(), List.of(),
            result.rowCount(), summary, AnalysisStatus.ANSWERED, startNanos, result);
    }

    /** 收尾：组装报告、写审计日志、返回。所有分支都走这里，保证审计不遗漏 */
    private AnalysisOutcome finish(
        String requestId, String question, List<String> linkedTables, String generatedSql,
        List<String> violations, Integer rowCount, String summary,
        AnalysisStatus status, long startNanos, QueryResult result
    ) {
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        AnalysisReport report = new AnalysisReport(
            requestId, question, linkedTables, generatedSql, violations,
            rowCount, summary, modelSummary, status, durationMs, LocalDateTime.now()
        );
        // 第 6 步：审计留痕后再返回
        auditLog.record(report);
        return new AnalysisOutcome(report, result);
    }

    /**
     * 报告 + 结果集的组合返回值。
     * 结果集不进审计报告（行数据可能很大且敏感），只在需要预览的场景透出。
     */
    public record AnalysisOutcome(AnalysisReport report, QueryResult result) {
    }
}
