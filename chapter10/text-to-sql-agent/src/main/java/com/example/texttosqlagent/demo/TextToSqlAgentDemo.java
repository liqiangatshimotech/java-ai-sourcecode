package com.example.texttosqlagent.demo;

import com.example.texttosqlagent.agent.AnalysisReport;
import com.example.texttosqlagent.agent.ConsoleAuditLogService;
import com.example.texttosqlagent.agent.DataAnalysisAgentService;
import com.example.texttosqlagent.agent.DataAnalysisAgentService.AnalysisOutcome;
import com.example.texttosqlagent.agent.RuleBasedSqlGenerationModel;
import com.example.texttosqlagent.agent.TemplateAnswerNarrator;
import com.example.texttosqlagent.config.ModelSettings;
import com.example.texttosqlagent.schema.MetricDictionary;
import com.example.texttosqlagent.schema.SchemaInspector;
import com.example.texttosqlagent.schema.SchemaLinkingResult;
import com.example.texttosqlagent.schema.SchemaLinkingService;
import com.example.texttosqlagent.schema.TableSchema;
import com.example.texttosqlagent.sql.QueryResult;
import com.example.texttosqlagent.sql.ReadOnlySqlExecutor;
import com.example.texttosqlagent.sql.SqlGuardResult;
import com.example.texttosqlagent.sql.SqlSafetyGuard;
import java.util.HashSet;
import java.util.List;

/**
 * 命令行离线演示：不启动 Spring 容器、不需要任何 API Key，
 * 手动 new 出全部组件，把 Agent 的每一步都打印到控制台。
 *
 * 演示 4 个问题：3 个黄金问题（正常出数）+ 1 个故意答不了的问题（展示诚实兜底）。
 *
 * 运行方式：
 *   mvn compile exec:java -Dexec.mainClass=com.example.texttosqlagent.demo.TextToSqlAgentDemo
 */
public class TextToSqlAgentDemo {

    private static final int MAX_ROWS = 200;

    public static void main(String[] args) throws Exception {
        // ---------- 1. 准备内嵌 H2 并初始化 schema.sql + data.sql ----------
        // 与 Web 模式共用 SELECT 账号工厂，演示链路也没有管理员写权限。
        javax.sql.DataSource dataSource =
            com.example.texttosqlagent.config.ReadOnlyDatabaseFactory.createDemo();

        // ---------- 2. 手动装配 Agent 全部组件（和 AgentConfiguration 里的规则一致） ----------
        ModelSettings settings = ModelSettings.deepSeekDefault();
        SchemaInspector inspector = new SchemaInspector(dataSource);
        List<TableSchema> tableSchemas = inspector.inspect();
        MetricDictionary metricDictionary = new MetricDictionary();
        SchemaLinkingService linkingService = new SchemaLinkingService(tableSchemas, metricDictionary);
        // 离线演示固定使用规则模型：没网没 key 也能完整跑通
        RuleBasedSqlGenerationModel model = new RuleBasedSqlGenerationModel();
        SqlSafetyGuard guard = new SqlSafetyGuard(MAX_ROWS, new HashSet<>(
            tableSchemas.stream().map(TableSchema::name).toList()));
        ReadOnlySqlExecutor executor = new ReadOnlySqlExecutor(dataSource, MAX_ROWS, 5);
        TemplateAnswerNarrator narrator = new TemplateAnswerNarrator();
        DataAnalysisAgentService agentService = new DataAnalysisAgentService(
            linkingService, model, guard, executor, narrator,
            new ConsoleAuditLogService(), "OFFLINE / rule-based / 离线规则模型");

        System.out.println("=".repeat(72));
        System.out.println("Text-to-SQL Agent 离线演示（规则模型，不调用任何外部 API）");
        System.out.println("对照真实模型路径的配置：" + settings.safeSummary());
        System.out.println("内置黄金问题：" + String.join("、", model.goldenQuestionNames()));
        System.out.println("=".repeat(72));

        // ---------- 3. 逐题演示：3 个黄金问题 + 1 个故意答不了的问题 ----------
        List<String> questions = List.of(
            "2026年6月销售额最高的五个商品分类",
            "累计消费金额最高的十个客户",
            "2026年6月各支付渠道的金额占比",
            "员工工资最高的五个人是谁"   // 库里没有员工/工资表，展示诚实拒答
        );
        for (String question : questions) {
            demonstrate(question, linkingService, model, guard, agentService);
        }
        System.out.println("=".repeat(72));
        System.out.println("演示结束：4 个问题全部按预期处理（3 个出数，1 个诚实拒答）。");
    }

    /** 跑一个问题，并把选表、SQL、护栏、行数、解说逐步打印出来 */
    private static void demonstrate(
        String question,
        SchemaLinkingService linkingService,
        RuleBasedSqlGenerationModel model,
        SqlSafetyGuard guard,
        DataAnalysisAgentService agentService) {

        System.out.println();
        System.out.println("问题：" + question);

        // 第 1 步：Schema Linking（单独再调一次只为展示中间产物，链路内部还会调一次）
        SchemaLinkingResult linking = linkingService.link(question);
        System.out.println("① 选表：" + (linking.selectedTables().isEmpty()
            ? "（没有命中任何表）"
            : linking.selectedTables().stream().map(TableSchema::name).toList()
                + "，命中关键词 " + linking.matchedKeywords()));

        // 第 2 步：生成 SQL
        String sql = linking.selectedTables().isEmpty()
            ? null
            : model.generateSql(question, linking.schemaContext());
        System.out.println("② 生成 SQL：" + (sql == null ? "（跳过，没有相关表）" : ""));
        if (sql != null) {
            System.out.println(indent(sql));
        }

        // 第 3 步：护栏校验（展示用；链路内部会再校验一次，以链路内的为准）
        if (sql != null) {
            SqlGuardResult guardResult = guard.check(sql);
            System.out.println("③ 护栏：" + (guardResult.passed()
                ? "通过"
                : "拦截，违规明细 " + guardResult.violations()));
        } else {
            System.out.println("③ 护栏：（跳过）");
        }

        // 第 4~6 步：走完整编排链路，拿最终报告
        AnalysisOutcome outcome = agentService.askWithResult(question);
        AnalysisReport report = outcome.report();
        System.out.println("④ 执行状态：" + report.status()
            + (report.rowCount() == null ? "" : "，行数 " + report.rowCount())
            + "，耗时 " + report.durationMs() + "ms");

        QueryResult result = outcome.result();
        if (result != null && result.rowCount() > 0) {
            System.out.println("⑤ 结果预览：");
            result.rows().stream().limit(5).forEach(row -> System.out.println("   " + row));
        }
        System.out.println("⑥ 中文解说：" + report.summary());
        System.out.println("-".repeat(72));
    }

    /** 多行 SQL 缩进展示，控制台里看得清结构 */
    private static String indent(String sql) {
        return sql.indent(4);
    }
}
