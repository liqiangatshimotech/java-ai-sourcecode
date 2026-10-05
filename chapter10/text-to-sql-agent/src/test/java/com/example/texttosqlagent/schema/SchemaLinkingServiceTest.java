package com.example.texttosqlagent.schema;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.texttosqlagent.TestDatabaseSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SchemaLinkingService 测试：验证「什么问题选什么表」符合业务直觉。
 * 选错表意味着模型拿到错误的上下文，后面生成 SQL 再准也白搭。
 */
class SchemaLinkingServiceTest {

    private SchemaLinkingService linkingService;

    @BeforeEach
    void setUp() {
        SchemaInspector inspector = new SchemaInspector(TestDatabaseSupport.newInitializedDataSource());
        linkingService = new SchemaLinkingService(inspector.inspect(), new MetricDictionary());
    }

    /** 断言辅助：把选中的表名统一转小写，规避 H2 大写表名的差异 */
    private List<String> lowerNames(SchemaLinkingResult result) {
        return result.selectedTables().stream().map(t -> t.name().toLowerCase()).toList();
    }

    @Test
    void salesRankingQuestionPicksOrderTablesNotCustomers() {
        // 「销售额 + 分类」应该选中订单三件套，但绝不该选客户表
        SchemaLinkingResult result = linkingService.link("2026年6月销售额最高的五个商品分类");
        List<String> tables = lowerNames(result);
        assertTrue(tables.contains("orders"), "应该选中 orders，实际：" + tables);
        assertTrue(tables.contains("order_items"), "应该选中 order_items，实际：" + tables);
        assertTrue(tables.contains("products"), "应该选中 products，实际：" + tables);
        assertFalse(tables.contains("customers"), "不该选中 customers，实际：" + tables);
        // schema 上下文里必须带建表片段，模型才有东西可参考
        assertTrue(result.schemaContext().contains("CREATE TABLE"));
    }

    @Test
    void customerSpendingQuestionPicksCustomersAndOrders() {
        SchemaLinkingResult result = linkingService.link("累计消费金额最高的十个客户");
        List<String> tables = lowerNames(result);
        assertTrue(tables.contains("customers"), "应该选中 customers，实际：" + tables);
        assertTrue(tables.contains("orders"), "应该选中 orders，实际：" + tables);
        // 客户消费不需要商品明细和支付表
        assertFalse(tables.contains("products"), "不该选中 products，实际：" + tables);
    }

    @Test
    void paymentChannelQuestionPicksPaymentsAndOrders() {
        SchemaLinkingResult result = linkingService.link("2026年6月各支付渠道的金额占比");
        List<String> tables = lowerNames(result);
        assertTrue(tables.contains("payments"), "应该选中 payments，实际：" + tables);
        assertTrue(tables.contains("orders"), "应该选中 orders，实际：" + tables);
    }

    @Test
    void averageOrderValueQuestionPicksOrdersAndProducts() {
        SchemaLinkingResult result = linkingService.link("每个商品分类的平均客单价");
        List<String> tables = lowerNames(result);
        assertTrue(tables.contains("orders"), "应该选中 orders，实际：" + tables);
        assertTrue(tables.contains("products"), "应该选中 products，实际：" + tables);
    }

    @Test
    void unrelatedQuestionSelectsNothing() {
        // 库里根本没有员工/工资表，选表结果必须为空，交给上层诚实拒答
        SchemaLinkingResult result = linkingService.link("员工工资是多少");
        assertTrue(result.selectedTables().isEmpty(),
            "无关问题不该选中任何表，实际：" + lowerNames(result));
    }
}
