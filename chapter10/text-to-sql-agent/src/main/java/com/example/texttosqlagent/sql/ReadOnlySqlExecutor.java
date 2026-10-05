package com.example.texttosqlagent.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 * 使用数据库 SELECT 账号执行经过护栏的查询，并控制返回行数和 JDBC 查询超时。
 * setReadOnly 仅表达驱动提示；executeQuery 仅表达预期结果类型，二者都不是授权边界。
 * 即使调用者绕过 SQL 护栏，数据库身份也不能具备业务写入、DDL 或外部函数权限。
 */
public class ReadOnlySqlExecutor {

    private final DataSource dataSource;
    private final int maxRows;
    private final int timeoutSeconds;

    public ReadOnlySqlExecutor(DataSource dataSource, int maxRows, int timeoutSeconds) {
        if (maxRows < 1 || maxRows > 10000 || timeoutSeconds < 1) {
            throw new IllegalArgumentException("行数和查询超时必须为正，行数不能超过 10000");
        }
        this.dataSource = dataSource;
        this.maxRows = maxRows;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 执行一条已经通过护栏的 SELECT，返回结果集快照。
     * 结果全部读进内存（教学库数据量小），返回后连接即关闭。
     */
    public QueryResult execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setReadOnly(true);
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(timeoutSeconds);
                // 多取一行：读到第 maxRows+1 行就知道还有数据没拿完
                statement.setMaxRows(maxRows + 1);
                try (ResultSet rs = statement.executeQuery(sql)) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    List<String> columns = new ArrayList<>();
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        // 优先用别名（黄金 SQL 里都是中文别名），解说时直接可读
                        columns.add(metaData.getColumnLabel(i));
                    }

                    List<List<Object>> rows = new ArrayList<>();
                    boolean truncated = false;
                    while (rs.next()) {
                        if (rows.size() == maxRows) {
                            // 这就是多取出来的那一行：只用来打截断标记，不进结果
                            truncated = true;
                            break;
                        }
                        List<Object> row = new ArrayList<>();
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            row.add(rs.getObject(i));
                        }
                        rows.add(row);
                    }
                    return new QueryResult(columns, rows, truncated);
                }
            }
        }
    }
}
