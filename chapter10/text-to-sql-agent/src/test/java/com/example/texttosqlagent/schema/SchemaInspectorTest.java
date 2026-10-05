package com.example.texttosqlagent.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.texttosqlagent.TestDatabaseSupport;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SchemaInspector 测试：验证 COMMENT ON 写的中文注释
 * 真的能被 JDBC DatabaseMetaData 的 REMARKS 读回来。
 * 这是整个语义层的地基——注释读不到，选表和 prompt 都是空中楼阁。
 */
class SchemaInspectorTest {

    private SchemaInspector inspector;

    @BeforeEach
    void setUp() {
        DataSource dataSource = TestDatabaseSupport.newInitializedDataSource();
        inspector = new SchemaInspector(dataSource);
    }

    @Test
    void readsAllFiveBusinessTables() {
        List<TableSchema> tables = inspector.inspect();
        assertEquals(5, tables.size());
        List<String> names = tables.stream().map(t -> t.name().toUpperCase()).toList();
        // H2 默认把未加引号的表名存成大写，断言时统一转大写比较
        assertTrue(names.containsAll(
            List.of("CUSTOMERS", "PRODUCTS", "ORDERS", "ORDER_ITEMS", "PAYMENTS")));
    }

    @Test
    void tableCommentsComeBackViaRemarks() {
        List<TableSchema> tables = inspector.inspect();
        for (TableSchema table : tables) {
            // 每张表都必须有中文注释，语义层不允许「裸表」
            assertFalse(table.comment() == null || table.comment().isBlank(),
                "表 " + table.name() + " 缺少注释");
        }
        TableSchema orders = tables.stream()
            .filter(t -> t.name().equalsIgnoreCase("orders")).findFirst().orElseThrow();
        assertTrue(orders.comment().contains("订单"));
    }

    @Test
    void columnCommentsComeBackViaRemarks() {
        List<TableSchema> tables = inspector.inspect();
        for (TableSchema table : tables) {
            assertFalse(table.columns().isEmpty(), "表 " + table.name() + " 没读到任何列");
            for (ColumnSchema column : table.columns()) {
                assertFalse(column.comment() == null || column.comment().isBlank(),
                    "列 " + table.name() + "." + column.name() + " 缺少注释");
                assertFalse(column.type().isBlank(), "列 " + column.name() + " 没读到类型");
            }
        }
    }

    @Test
    void ddlFragmentContainsChineseComments() {
        // 给 prompt 用的 DDL 片段必须带中文注释，模型才知道字段的业务含义
        List<TableSchema> tables = inspector.inspect();
        TableSchema products = tables.stream()
            .filter(t -> t.name().equalsIgnoreCase("products")).findFirst().orElseThrow();
        String ddl = products.toDdlFragment();
        assertTrue(ddl.contains("CREATE TABLE"));
        assertTrue(ddl.contains("商品分类"));
    }
}
