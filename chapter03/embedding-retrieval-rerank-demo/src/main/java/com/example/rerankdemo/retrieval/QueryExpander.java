package com.example.rerankdemo.retrieval;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询扩展。
 *
 * <p>召回阶段经常需要补同义词、业务别名和缩写。这里用规则演示，生产环境可以来自：
 * 业务词典、历史搜索日志、LLM 改写、用户画像或多轮对话上下文。
 */
public final class QueryExpander {

    public String expand(String query) {
        List<String> additions = new ArrayList<>();
        if (TextSignals.containsAny(query, List.of("报销凭证", "票据"))) {
            additions.add("发票");
            additions.add("开票");
            additions.add("抬头");
        }
        if (TextSignals.containsAny(query, List.of("公司名称", "企业名称"))) {
            additions.add("抬头");
            additions.add("主体名称");
        }
        if (TextSignals.containsAny(query, List.of("重新开", "重开", "更正"))) {
            additions.add("重开");
            additions.add("更正");
        }

        if (additions.isEmpty()) {
            return query;
        }
        return query + " " + String.join(" ", additions);
    }
}
