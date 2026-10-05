package com.example.texttosqlagent.agent;

import com.example.texttosqlagent.sql.QueryResult;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模板化结果解说器：不调模型，用固定模板把结果集翻译成中文。
 *
 * 解说策略（刻意保持简单透明）：
 * 1. 先概括总行数，结果触达上限时诚实说明「只是前 N 行」；
 * 2. 列出前 3 行内容，列名直接用 SQL 里的中文别名，读者不用猜字段含义；
 * 3. 结果恰好是「类别 + 数值」两列时，补一句可视化建议（柱状图/饼图）。
 *
 * 真实系统可以把这一步换成「让大模型基于结果写结论」，
 * 但那时务必把结果集（而不是整库）喂给模型，避免它编造数字。
 */
public class TemplateAnswerNarrator implements AnswerNarrator {

    /** 解说里最多展示的数据行数 */
    private static final int PREVIEW_ROWS = 3;

    @Override
    public String narrate(String question, QueryResult result) {
        if (result.rowCount() == 0) {
            return "查询执行成功，但没有找到符合条件的数据。";
        }

        StringBuilder narration = new StringBuilder();
        narration.append("共查询到 ").append(result.rowCount()).append(" 行结果");
        if (result.truncated()) {
            // 结果被截断必须明说，不然用户会把「前 N 行」当成全量
            narration.append("（数据较多，这里仅展示前 ").append(result.rowCount()).append(" 行）");
        }
        narration.append("。");

        // 逐行列出前 PREVIEW_ROWS 行：「列名=值」对，逗号分隔
        List<List<Object>> preview = result.rows().stream().limit(PREVIEW_ROWS).toList();
        for (int i = 0; i < preview.size(); i++) {
            narration.append("\n第 ").append(i + 1).append(" 行：");
            List<Object> row = preview.get(i);
            narration.append(java.util.stream.IntStream.range(0, result.columns().size())
                .mapToObj(col -> result.columns().get(col) + "=" + row.get(col))
                .collect(Collectors.joining("，")));
        }

        // 「类别 + 数值」两列结构是图表的最佳输入，顺手给一句可视化建议
        if (result.columns().size() == 2 && secondColumnIsNumeric(result)) {
            narration.append("\n这个结果只有「类别 + 数值」两个维度，适合用柱状图/饼图展示。");
        }
        return narration.toString();
    }

    /** 判断第二列是不是数值列（所有行都是 Number 才算） */
    private boolean secondColumnIsNumeric(QueryResult result) {
        return result.rows().stream()
            .allMatch(row -> row.size() > 1 && row.get(1) instanceof Number);
    }
}
