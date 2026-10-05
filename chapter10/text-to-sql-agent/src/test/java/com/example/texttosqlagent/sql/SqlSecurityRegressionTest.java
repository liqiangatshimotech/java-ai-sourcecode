package com.example.texttosqlagent.sql;

import static org.junit.jupiter.api.Assertions.*;
import com.example.texttosqlagent.TestDatabaseSupport;
import java.sql.SQLException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 这些用例来自旧正则护栏的漏检形态；只验证外部安全行为，不镜像实现步骤。 */
class SqlSecurityRegressionTest {
    private final SqlSafetyGuard guard = new SqlSafetyGuard(2, Set.of("customers", "orders"));

    @ParameterizedTest
    @ValueSource(strings = {
        "SELECT * FROM orders, secrets",
        "SELECT * FROM \"SECRETS\"",
        "SELECT * FROM private.orders",
        "SELECT * FROM \"private\".\"ORDERS\"",
        "SELECT * FROM \"PUBLIC.orders\"",
        "SELECT * FROM orders WHERE EXISTS (SELECT 1 FROM secrets)",
        "SELECT (SELECT id FROM secrets) FROM orders",
        "SELECT * FROM orders UNION SELECT * FROM secrets",
        "WITH safe AS (SELECT * FROM secrets) SELECT * FROM safe",
        "WITH secrets AS (SELECT * FROM secrets) SELECT * FROM secrets",
        "SELECT * FROM (WITH secrets AS (SELECT * FROM orders) SELECT * FROM secrets) x, secrets",
        "WITH x AS (SELECT * FROM later), later AS (SELECT * FROM orders) SELECT * FROM x",
        "WITH RECURSIVE x AS (SELECT * FROM orders) SELECT * FROM x",
        "SELECT CSVWRITE('/tmp/leak', 'SELECT * FROM orders')",
        "SELECT FILE_READ('/etc/passwd')",
        "SELECT NEXT VALUE FOR seq",
        "SELECT * FROM CSVREAD('/tmp/data')",
        "SELECT * FROM orders WHERE id = 1 OR SLEEP(5) = 0",
        "SELECT * FROM orders ORDER BY SLEEP(5)",
        "SELECT id, SUM(id) OVER (PARTITION BY SLEEP(5)) FROM orders",
        "SELECT name FROM customers WHERE name LIKE '%' ESCAPE FILE_READ('/tmp/data')",
        "SELECT * REPLACE (FILE_READ('/tmp/data') AS id) FROM orders",
        "SELECT * FROM orders LIMIT 2; DELETE FROM orders",
        "SELECT * INTO copied_orders FROM orders",
        "SELECT * FROM orders FOR UPDATE",
        "SELECT * FROM orders LIMIT ?",
        "SELECT * FROM orders LIMIT -1"
    })
    void rejectsUnsafeOrUnreviewedSyntax(String sql) {
        assertFalse(guard.check(sql).passed(), sql);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SELECT * FROM \"PUBLIC\".\"ORDERS\"",
        "SELECT * FROM orders o, customers c WHERE o.customer_id = c.id",
        "SELECT '--; /* DELETE */' AS text FROM orders",
        "SELECT * FROM orders /* a\n multi-line comment */",
        "WITH recent AS (SELECT * FROM orders), result AS (SELECT * FROM recent) SELECT * FROM result",
        "WITH orders AS (SELECT * FROM PUBLIC.orders) SELECT * FROM orders",
        "SELECT * FROM (SELECT * FROM orders) x",
        "SELECT * FROM orders WHERE EXISTS (SELECT 1 FROM customers)",
        "SELECT * FROM orders UNION SELECT * FROM orders"
    })
    void preservesSupportedQueries(String sql) {
        SqlGuardResult result = guard.check(sql);
        assertTrue(result.passed(), sql + " -> " + result.violations());
    }

    @Test
    void guardAndExecutorDetectActualTruncationTogether() throws SQLException {
        var executor = new ReadOnlySqlExecutor(TestDatabaseSupport.newInitializedDataSource(), 2, 5);
        // 原实现加 LIMIT 2 后，执行器永远读不到第 3 行，导致 truncated 永远为 false。
        var result = executor.execute(guard.check("SELECT id FROM customers ORDER BY id").sanitizedSql());
        assertEquals(2, result.rowCount());
        assertTrue(result.truncated());
        // 用户只要 2 行时，LIMIT 2 属于查询定义，并非系统预算造成的截断。
        assertFalse(executor.execute(guard.check("SELECT id FROM customers LIMIT 2").sanitizedSql()).truncated());
    }

    @Test
    void onlyCapsTopLevelLimitAndPreservesOffset() throws SQLException {
        var executor = new ReadOnlySqlExecutor(TestDatabaseSupport.newInitializedDataSource(), 2, 5);
        String sql = guard.check("SELECT * FROM (SELECT id FROM customers LIMIT 8) x ORDER BY id LIMIT 1, 9999999999999999999999").sanitizedSql();
        assertNotNull(sql);
        assertTrue(sql.contains("LIMIT 8"), sql);
        QueryResult result = executor.execute(sql);
        assertEquals(2L, ((Number) result.rows().get(0).get(0)).longValue());
        assertTrue(result.truncated());
    }

    @Test
    void databaseDeniesWritesAndDataStaysUnchangedEvenWithoutGuard() throws SQLException {
        var source = TestDatabaseSupport.newInitializedDataSource();
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            // 特意不用 executeQuery，也不调用 setReadOnly；验证的是数据库授权。
            for (String sql : new String[]{"DELETE FROM customers", "UPDATE customers SET name='changed'",
                "DROP TABLE customers", "CREATE TABLE PUBLIC.evil(id INT)",
                "INSERT INTO customers(id,name,level,city,registered_at) VALUES(99,'x','x','x',CURRENT_TIMESTAMP)"}) {
                assertThrows(SQLException.class, () -> statement.execute(sql), sql);
            }
            try (var rs = statement.executeQuery("SELECT COUNT(*), MIN(name) FROM customers")) {
                assertTrue(rs.next());
                assertEquals(12, rs.getInt(1));
            }
            try (var rs = statement.executeQuery("SELECT name FROM customers WHERE id=1")) {
                assertTrue(rs.next());
                assertEquals("张伟", rs.getString(1));
            }
        }
    }
}
