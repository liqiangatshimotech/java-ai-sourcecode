package com.example.texttosqlagent.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * 演示库把建库身份与运行身份拆开：管理员仅在启动方法的局部变量中存在。
 * 返回给 SchemaInspector / 执行器的连接只拥有五张业务表的 SELECT 权限。
 * 这才是数据库会实际执行的授权；JDBC readOnly 提示不能替代 GRANT。
 */
public final class ReadOnlyDatabaseFactory {
    private ReadOnlyDatabaseFactory() { }

    public static DataSource createDemo() {
        String url = "jdbc:h2:mem:text2sql-" + UUID.randomUUID();
        String readerPassword = UUID.randomUUID().toString();
        DriverManagerDataSource administrator = new DriverManagerDataSource(url + ";DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection connection = administrator.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
            try (var statement = connection.createStatement()) {
                // 密码由程序随机生成，不接受模型/用户文本，不记录到日志或代码包。
                statement.execute("CREATE USER course_reader PASSWORD '" + readerPassword + "'");
                String tables = com.example.texttosqlagent.schema.BusinessTablePolicy.NAMES.stream()
                    .sorted().map(name -> "PUBLIC." + name).collect(java.util.stream.Collectors.joining(", "));
                statement.execute("GRANT SELECT ON " + tables + " TO course_reader");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("初始化演示库与只读身份失败", exception);
        }
        // reader URL 不包含 DB_CLOSE_DELAY 等需要管理员权限的数据库设置。
        return new DriverManagerDataSource(url, "course_reader", readerPassword);
    }
}
