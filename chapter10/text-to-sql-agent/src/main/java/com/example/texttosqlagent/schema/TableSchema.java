package com.example.texttosqlagent.schema;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 一张表的元数据：表名、表注释、列清单。
 *
 * toDdlFragment() 把它还原成一段带中文注释的「建表语句片段」，
 * 这段文本会原样放进给大模型的 system prompt 里——
 * 模型不需要看到真实数据库，看这段 DDL 就知道有哪些表和字段可用。
 */
public record TableSchema(String name, String comment, List<ColumnSchema> columns) {

    /**
     * 生成给 prompt 用的 DDL 片段，例如：
     * CREATE TABLE orders ( -- 订单表，记录客户下单的订单状态和订单总额
     *   id BIGINT, -- 订单ID
     *   ...
     * );
     */
    public String toDdlFragment() {
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE ").append(name);
        if (comment != null && !comment.isBlank()) {
            // 表注释放在表名后面，模型第一眼就能看到这张表是干什么的
            ddl.append(" -- ").append(comment);
        }
        ddl.append(" (\n");
        ddl.append(columns.stream()
            .map(column -> {
                String line = "  " + column.name() + " " + column.type();
                if (column.comment() != null && !column.comment().isBlank()) {
                    line += " -- " + column.comment();
                }
                return line;
            })
            .collect(Collectors.joining(",\n")));
        ddl.append("\n);");
        return ddl.toString();
    }
}
