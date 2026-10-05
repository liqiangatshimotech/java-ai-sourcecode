package com.example.texttosqlagent.sql;

import java.util.List;

/**
 * 一次只读查询的结果集快照。
 *
 * @param columns   列名清单（按 ResultSet 顺序）
 * @param rows      数据行，每行是与 columns 对齐的值列表
 * @param truncated 是否因为触达 maxRows 上限被截断；
 *                  true 时解说层应该提醒用户「结果只是前 N 行」
 */
public record QueryResult(List<String> columns, List<List<Object>> rows, boolean truncated) {

    public int rowCount() {
        return rows.size();
    }
}
