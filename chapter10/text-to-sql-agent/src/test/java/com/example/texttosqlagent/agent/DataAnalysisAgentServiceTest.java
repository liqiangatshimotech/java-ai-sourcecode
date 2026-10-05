package com.example.texttosqlagent.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.texttosqlagent.TestDatabaseSupport;
import com.example.texttosqlagent.schema.MetricDictionary;
import com.example.texttosqlagent.schema.SchemaInspector;
import com.example.texttosqlagent.schema.SchemaLinkingService;
import com.example.texttosqlagent.schema.TableSchema;
import com.example.texttosqlagent.sql.ReadOnlySqlExecutor;
import com.example.texttosqlagent.sql.SqlSafetyGuard;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * DataAnalysisAgentService 端到端测试：
 * 真实 H2 + 真实选表/护栏/执行器，只有「模型」这一环用离线规则模型或假模型。
 * 全部离线，不需要任何 API Key。
 */
class DataAnalysisAgentServiceTest {

    private DataSource dataSource;
    private List<AnalysisReport> auditTrail;
    private List<TableSchema> tableSchemas;
    private SchemaLinkingService linkingService;
    private SqlSafetyGuard guard;
    private ReadOnlySqlExecutor executor;

    @BeforeEach
    void setUp() {
        dataSource = TestDatabaseSupport.newInitializedDataSource();
        auditTrail = new ArrayList<>();
        SchemaInspector inspector = new SchemaInspector(dataSource);
        tableSchemas = inspector.inspect();
        linkingService = new SchemaLinkingService(tableSchemas, new MetricDictionary());
        guard = new SqlSafetyGuard(200, new HashSet<>(
            tableSchemas.stream().map(TableSchema::name).toList()));
        executor = new ReadOnlySqlExecutor(dataSource, 200, 5);
    }

    /** 用给定的「模型」装配一条完整链路；审计日志收集到列表里方便断言 */
    private DataAnalysisAgentService buildAgent(SqlGenerationModel model) {
        return new DataAnalysisAgentService(
            linkingService, model, guard, executor,
            new TemplateAnswerNarrator(), auditTrail::add, "TEST / rule-based / 测试桩");
    }

    @Test
    void goldenQuestionsAreAnsweredWithRows() {
        DataAnalysisAgentService agent = buildAgent(new RuleBasedSqlGenerationModel());
        // 四道黄金问题全部要能出数
        List<String> questions = List.of(
            "2026年6月销售额最高的五个商品分类",
            "累计消费金额最高的十个客户",
            "2026年6月各支付渠道的金额占比",
            "每个商品分类的平均客单价"
        );
        for (String question : questions) {
            AnalysisReport report = agent.ask(question);
            assertEquals(AnalysisStatus.ANSWERED, report.status(), "问题应被回答：" + question);
            assertNotNull(report.rowCount());
            assertTrue(report.rowCount() > 0, "结果应该有数据行：" + question);
            assertFalse(report.summary().isBlank(), "中文解说不能为空：" + question);
            assertFalse(report.linkedTables().isEmpty(), "应该选中相关表：" + question);
        }
        // 第一题只有 4 个分类，结果行数应该恰好是 4
        AnalysisReport first = agent.ask(questions.get(0));
        assertEquals(4, first.rowCount());
        // 每次提问都必须留审计痕迹
        assertTrue(auditTrail.size() >= questions.size());
    }

    @Test
    void unrelatedQuestionGetsHonestNoMatchingTable() {
        DataAnalysisAgentService agent = buildAgent(new RuleBasedSqlGenerationModel());
        // 库里没有员工/工资概念，必须诚实拒答而不是硬编 SQL
        AnalysisReport report = agent.ask("员工工资是多少");
        assertTrue(
            report.status() == AnalysisStatus.NO_MATCHING_TABLE
                || report.status() == AnalysisStatus.CANNOT_ANSWER,
            "无关问题应该拒答，实际状态：" + report.status());
        assertEquals(AnalysisStatus.NO_MATCHING_TABLE, report.status());
        assertNull(report.generatedSql(), "没找到表就不该生成 SQL");
        assertTrue(report.summary().contains("找不到") || report.summary().contains("没有找到"));
    }

    @Test
    void unmatchableQuestionGetsCannotAnswer() {
        DataAnalysisAgentService agent = buildAgent(new RuleBasedSqlGenerationModel());
        // 「订单量」能选中 orders 表，但规则模型没有这道题——拒答而不是编 SQL
        AnalysisReport report = agent.ask("订单量是多少");
        assertEquals(AnalysisStatus.CANNOT_ANSWER, report.status());
        assertNull(report.rowCount());
    }

    @Test
    void dangerousSqlIsBlockedAndNeverExecuted() throws Exception {
        // 假模型：不管问什么都返回一条 DELETE，模拟模型被注入/失控的场景
        SqlGenerationModel evilModel = (question, schemaContext) -> "DELETE FROM customers";
        DataAnalysisAgentService agent = buildAgent(evilModel);

        AnalysisReport report = agent.ask("2026年6月销售额最高的五个商品分类");
        assertEquals(AnalysisStatus.BLOCKED_BY_GUARD, report.status());
        assertFalse(report.violations().isEmpty(), "护栏必须给出违规明细");
        assertNull(report.rowCount(), "被拦截的 SQL 不该有执行结果");

        // 关键断言：危险 SQL 没有真的执行——客户表 12 行一行没少
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM customers")) {
            rs.next();
            assertEquals(12, rs.getInt(1), "被护栏拦截的 DELETE 不应该真的执行");
        }
    }

    @Test
    void invalidSqlBecomesExecutionFailed() {
        // 假模型生成一条语法合法、能过护栏，但引用了不存在列的 SQL
        SqlGenerationModel hallucinatingModel =
            (question, schemaContext) -> "SELECT no_such_column FROM orders LIMIT 5";
        DataAnalysisAgentService agent = buildAgent(hallucinatingModel);

        AnalysisReport report = agent.ask("2026年6月销售额最高的五个商品分类");
        assertEquals(AnalysisStatus.EXECUTION_FAILED, report.status());
        // 回给调用方的是脱敏说明，不带原始异常栈
        assertFalse(report.summary().contains("Exception"));
        assertTrue(report.summary().contains("查询执行失败"));
    }
}
