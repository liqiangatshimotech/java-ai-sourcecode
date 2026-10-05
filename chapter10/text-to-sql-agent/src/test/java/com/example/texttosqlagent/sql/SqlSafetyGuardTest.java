package com.example.texttosqlagent.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SqlSafetyGuard 测试：每条护栏规则至少一个用例。
 * 护栏是安全底线，规则改任何一条都要先过这些测试。
 */
class SqlSafetyGuardTest {

    private static final Set<String> WHITELIST =
        Set.of("customers", "products", "orders", "order_items", "payments");

    private SqlSafetyGuard guard;

    @BeforeEach
    void setUp() {
        guard = new SqlSafetyGuard(200, WHITELIST);
    }

    @Test
    void blocksDropTable() {
        // 经典破坏语句：DROP 关键字 + 非 SELECT 开头，双重违规
        SqlGuardResult result = guard.check("DROP TABLE customers");
        assertFalse(result.passed());
        assertNull(result.sanitizedSql());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("DROP")),
            "违规明细里要指出 DROP：" + result.violations());
    }

    @Test
    void blocksDelete() {
        SqlGuardResult result = guard.check("DELETE FROM orders WHERE id = 1");
        assertFalse(result.passed());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("DELETE")));
    }

    @Test
    void blocksInsert() {
        SqlGuardResult result = guard.check("INSERT INTO orders (id) VALUES (999)");
        assertFalse(result.passed());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("INSERT")));
    }

    @Test
    void blocksMultipleStatements() {
        // 注入常见手法：第一条正常查询后面偷偷跟一条写操作
        SqlGuardResult result = guard.check(
            "SELECT * FROM orders LIMIT 5; DELETE FROM orders");
        assertFalse(result.passed());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("一条 SQL")),
            "应该报多语句违规：" + result.violations());
    }

    @Test
    void blocksNonWhitelistedTable() {
        // 模型幻觉出不存在的表，或者试图查越权表，都要拦下
        SqlGuardResult result = guard.check("SELECT * FROM employees LIMIT 5");
        assertFalse(result.passed());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("employees")));
    }

    @Test
    void blocksIntoOutfile() {
        SqlGuardResult result = guard.check(
            "SELECT * FROM orders INTO OUTFILE '/tmp/dump.csv'");
        assertFalse(result.passed());
        assertTrue(result.violations().stream().anyMatch(v -> v.contains("OUTFILE")));
    }

    @Test
    void appendsLimitWhenMissing() {
        SqlGuardResult result = guard.check("SELECT * FROM orders");
        assertTrue(result.passed(), "合法 SELECT 应该通过：" + result.violations());
        // 没写 LIMIT 自动补到 maxRows
        assertTrue(result.sanitizedSql().endsWith("LIMIT 201"),
            "应该自动补 LIMIT 201：" + result.sanitizedSql());
    }

    @Test
    void clampsOversizedLimit() {
        SqlGuardResult result = guard.check("SELECT * FROM orders LIMIT 99999");
        assertTrue(result.passed());
        // 超限的 LIMIT 收紧到 maxRows，而不是直接拒绝——查询意图本身没错
        assertTrue(result.sanitizedSql().contains("LIMIT 201"),
            "应该收紧为 LIMIT 201：" + result.sanitizedSql());
        assertFalse(result.sanitizedSql().contains("99999"));
    }

    @Test
    void keepsReasonableLimit() {
        SqlGuardResult result = guard.check("SELECT * FROM orders LIMIT 50");
        assertTrue(result.passed());
        assertTrue(result.sanitizedSql().contains("LIMIT 50"));
    }

    @Test
    void allowsWithClause() {
        // WITH（CTE）是合法的只读写法；CTE 名本身不算白名单外的「表」
        SqlGuardResult result = guard.check(
            "WITH june_orders AS (SELECT * FROM orders WHERE created_at >= '2026-06-01') "
                + "SELECT * FROM june_orders");
        assertTrue(result.passed(), "WITH 查询应该放行：" + result.violations());
        assertTrue(result.sanitizedSql().contains("LIMIT 201"));
    }

    @Test
    void stripsCommentsBeforeChecking() {
        // 注释里的危险关键字不该误伤；剥离后这是一条完全合法的查询
        SqlGuardResult result = guard.check(
            "SELECT * FROM orders -- 顺手把 customers DROP 掉\n/* DELETE FROM payments */ LIMIT 3");
        assertTrue(result.passed(), "注释剥离后应该通过：" + result.violations());
        assertTrue(result.sanitizedSql().contains("LIMIT 3"));
    }

    @Test
    void wordBoundaryDoesNotMisfireOnColumnNames() {
        // created_at 里包含 create 的字形，但词边界匹配不能误伤它
        SqlGuardResult result = guard.check(
            "SELECT id, created_at FROM orders ORDER BY created_at DESC LIMIT 10");
        assertTrue(result.passed(), "created_at 不该触发 CREATE 拦截：" + result.violations());
    }

    @Test
    void tableNamesAreCaseInsensitive() {
        // H2 存大写表名、模型可能写小写，白名单比较必须大小写不敏感
        SqlGuardResult result = guard.check("SELECT * FROM ORDERS o JOIN Order_Items i ON i.order_id = o.id");
        assertTrue(result.passed(), "大小写混合的表名应该通过：" + result.violations());
    }

    @Test
    void blocksEmptySql() {
        SqlGuardResult result = guard.check("   ");
        assertFalse(result.passed());
        assertEquals(1, result.violations().size());
    }
}
