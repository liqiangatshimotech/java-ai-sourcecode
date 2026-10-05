package com.example.texttosqlagent.schema;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Schema 读取器：通过 JDBC DatabaseMetaData 把库里的表结构和中文注释读出来。
 *
 * 为什么不手写一份表结构配置？因为「语义跟着 schema 走」——
 * schema.sql 里的 COMMENT ON 是唯一事实来源，代码里再抄一份迟早会漂移。
 * H2 会把 COMMENT ON 的内容存进元数据，DatabaseMetaData 的 REMARKS 列能读回来。
 */
public class SchemaInspector {

    private final DataSource dataSource;

    public SchemaInspector(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 读取 PUBLIC 下真实存在且属于 BusinessTablePolicy 授权范围的业务表。
     * 返回按表名排序的 TableSchema 列表，顺序稳定，方便拼 prompt 和做测试断言。
     */
    public List<TableSchema> inspect() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            // 先读表：表名 -> 表注释，用 LinkedHashMap 保持读取顺序
            Map<String, String> tables = new LinkedHashMap<>();
            try (ResultSet rs = metaData.getTables(null, "PUBLIC", null, new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    // 元数据返回的表可能超出当前业务权限。这里只取授权范围与真实表的交集。
                    if (BusinessTablePolicy.NAMES.contains(tableName)) {
                        tables.put(tableName, rs.getString("REMARKS"));
                    }
                }
            }
            // 再逐表读列：列名、类型、列注释
            List<TableSchema> result = new ArrayList<>();
            for (Map.Entry<String, String> table : tables.entrySet()) {
                List<ColumnSchema> columns = new ArrayList<>();
                try (ResultSet rs = metaData.getColumns(null, "PUBLIC", table.getKey(), null)) {
                    while (rs.next()) {
                        columns.add(new ColumnSchema(
                            rs.getString("COLUMN_NAME"),
                            rs.getString("TYPE_NAME"),
                            rs.getString("REMARKS")
                        ));
                    }
                }
                result.add(new TableSchema(table.getKey(), table.getValue(), columns));
            }
            // H2 默认把未加引号的标识符存成大写，这里统一排序保证输出稳定
            result.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
            return result;
        } catch (SQLException e) {
            // 启动期读不到 schema 是致命错误，直接抛出让应用启动失败，而不是带病运行
            throw new IllegalStateException("读取数据库 schema 元数据失败", e);
        }
    }

    /**
     * 白名单用的真实表名集合（原样大小写，护栏比较时再统一转小写）。
     */
    public List<String> tableNames() {
        return inspect().stream().map(TableSchema::name).toList();
    }
}
