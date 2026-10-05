package com.example.texttosqlagent.schema;

import java.util.List;

/**
 * 业务指标词典（静态配置）。
 *
 * 大模型并不懂「销售额」「客单价」这种业务黑话到底怎么算，
 * 词典的作用就是把业务口径钉死：名称、业务定义、涉及哪些表、SQL 表达式。
 * 它同时服务两个环节：
 * 1. Schema Linking：用关键词命中来判断问题涉及哪些表；
 * 2. prompt 拼装：把指标口径告诉模型，避免它自由发挥算错口径。
 */
public class MetricDictionary {

    /**
     * 一个业务指标的完整定义。
     *
     * @param name           指标名（如「销售额」）
     * @param definition     业务定义，写清统计口径（如「排除退款订单」）
     * @param keywords       触发关键词，问题里出现任意一个即认为命中该指标
     * @param requiredTables 计算该指标需要的表（小写表名）
     * @param sqlExpression  推荐的 SQL 表达式，会写进 prompt 供模型参考
     */
    public record MetricDefinition(
        String name,
        String definition,
        List<String> keywords,
        List<String> requiredTables,
        String sqlExpression
    ) {
    }

    /** 课程里内置的五个核心指标，覆盖演示用的全部黄金问题 */
    private static final List<MetricDefinition> METRICS = List.of(
        new MetricDefinition(
            "销售额",
            "成交商品金额合计，排除已退款订单",
            List.of("销售额", "销售", "成交金额"),
            List.of("orders", "order_items", "products"),
            "SUM(order_items.quantity * order_items.unit_price)"
        ),
        new MetricDefinition(
            "客单价",
            "平均每笔订单的订单总额，排除已退款订单",
            List.of("客单价", "平均订单金额"),
            List.of("orders"),
            "AVG(orders.total_amount)"
        ),
        new MetricDefinition(
            "订单量",
            "订单数量合计，排除已退款订单",
            List.of("订单量", "订单数", "下单量"),
            List.of("orders"),
            "COUNT(DISTINCT orders.id)"
        ),
        new MetricDefinition(
            "支付金额",
            "按支付渠道统计的实付金额，排除已退款订单",
            List.of("支付", "渠道", "支付金额", "占比"),
            List.of("payments", "orders"),
            "SUM(payments.amount)"
        ),
        new MetricDefinition(
            "消费金额",
            "客户累计消费总额，排除已退款订单",
            List.of("消费", "累计", "客户"),
            List.of("customers", "orders"),
            "SUM(orders.total_amount)"
        )
    );

    /** 返回全部内置指标定义 */
    public List<MetricDefinition> all() {
        return METRICS;
    }

    /**
     * 找出问题命中了哪些指标：问题文本包含指标任意一个关键词即命中。
     * 子串匹配是最朴素的做法，真实系统这一层可以换成向量召回。
     */
    public List<MetricDefinition> findMatching(String question) {
        return METRICS.stream()
            .filter(metric -> metric.keywords().stream().anyMatch(question::contains))
            .toList();
    }

    /**
     * 把指标词典渲染成给 prompt 看的文本片段，让模型按统一口径算数。
     */
    public String toPromptFragment() {
        StringBuilder fragment = new StringBuilder("【业务指标口径】\n");
        for (MetricDefinition metric : METRICS) {
            fragment.append("- ").append(metric.name())
                .append("：").append(metric.definition())
                .append("；涉及表：").append(String.join(", ", metric.requiredTables()))
                .append("；SQL 表达式：").append(metric.sqlExpression())
                .append("\n");
        }
        return fragment.toString();
    }
}
