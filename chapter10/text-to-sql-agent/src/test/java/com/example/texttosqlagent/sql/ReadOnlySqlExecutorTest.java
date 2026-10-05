package com.example.texttosqlagent.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.texttosqlagent.TestDatabaseSupport;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * ReadOnlySqlExecutor 测试：正常取数、行数截断、只读约束。
 */
class ReadOnlySqlExecutorTest {

    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = TestDatabaseSupport.newInitializedDataSource();
    }

    @Test
    void returnsColumnsAndRows() throws SQLException {
        ReadOnlySqlExecutor executor = new ReadOnlySqlExecutor(dataSource, 200, 5);
        QueryResult result = executor.execute("SELECT id, name FROM customers ORDER BY id LIMIT 3");
        assertEquals(2, result.columns().size());
        assertEquals(3, result.rowCount());
        assertFalse(result.truncated());
        // H2 未加别名的列名返回大写
        assertEquals("ID", result.columns().get(0));
        assertEquals("NAME", result.columns().get(1));
        // 第一行是 id=1 的张伟（种子数据是确定的）
        assertEquals(1L, ((Number) result.rows().get(0).get(0)).longValue());
        assertEquals("张伟", result.rows().get(0).get(1));
    }

    @Test
    void marksTruncationWhenRowsExceedMaxRows() throws SQLException {
        // maxRows=2：库里 12 个客户，多取的第三行只用来打截断标记
        ReadOnlySqlExecutor executor = new ReadOnlySqlExecutor(dataSource, 2, 5);
        QueryResult result = executor.execute("SELECT id FROM customers ORDER BY id");
        assertEquals(2, result.rowCount());
        assertTrue(result.truncated(), "超过 maxRows 必须打截断标记");
    }

    @Test
    void noTruncationWhenRowsFit() throws SQLException {
        ReadOnlySqlExecutor executor = new ReadOnlySqlExecutor(dataSource, 200, 5);
        QueryResult result = executor.execute("SELECT id FROM customers WHERE id > 100");
        assertEquals(0, result.rowCount());
        assertFalse(result.truncated());
    }

    @Test
    void rejectsWriteStatementsWithSelectOnlyIdentity() {
        // 此测试只覆盖执行器报错路径；数据库权限与数据未变由 SqlSecurityRegressionTest 独立验证。
        ReadOnlySqlExecutor executor = new ReadOnlySqlExecutor(dataSource, 200, 5);
        assertThrows(SQLException.class,
            () -> executor.execute("INSERT INTO customers (id, name, level, city, registered_at) "
                + "VALUES (99, '黑客', '普通', '北京', '2026-01-01 00:00:00')"));
    }

    @Test
    void propagatesSqlExceptionToCaller() {
        // 查询不存在的列：异常必须原样抛给上层，由 Service 转成 EXECUTION_FAILED
        ReadOnlySqlExecutor executor = new ReadOnlySqlExecutor(dataSource, 200, 5);
        assertThrows(SQLException.class,
            () -> executor.execute("SELECT no_such_column FROM customers"));
    }
}
