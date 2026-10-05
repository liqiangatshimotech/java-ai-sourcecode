package com.example.texttosqlagent.agent;

import java.util.List;

/**
 * 离线规则模型：不调用任何外部 API 的 SQL 生成「模型」。
 *
 * 真实系统里这一层是大模型做语义理解；这里是它的离线替身：
 * 内置四道「黄金问题」，每道绑定若干关键词组（组内是「或」，组间是「与」），
 * 全部组命中就返回事先写好、经过人工核对的标准 SQL；
 * 一道都没命中就诚实返回 CANNOT_ANSWER——宁可拒答，不编 SQL。
 *
 * 它存在的意义：
 * 1. 课堂演示和 CI 测试在没有 API Key、没有网络时也能跑通完整链路；
 * 2. 给「真实模型路径」提供一个稳定可预期的对照组。
 */
public class RuleBasedSqlGenerationModel implements SqlGenerationModel {

    /**
     * 一道黄金问题的定义。
     *
     * @param name          问题名（审计和演示时展示用）
     * @param keywordGroups 关键词组：组内任一关键词出现即该组命中，所有组都命中才算匹配
     * @param sql           人工核对过的标准 SQL
     */
    private record GoldenQuery(String name, List<List<String>> keywordGroups, String sql) {

        /** 所有关键词组都命中（组内是或、组间是与）才算匹配 */
        boolean matches(String question) {
            return keywordGroups.stream()
                .allMatch(group -> group.stream().anyMatch(question::contains));
        }
    }

    /** 四道黄金问题，覆盖电商运营最常见的四类分析 */
    private static final List<GoldenQuery> GOLDEN_QUERIES = List.of(
        // 1. 分类销售额排名：6月 + 销售额 + 分类/品类
        new GoldenQuery(
            "6月分类销售额TOP5",
            List.of(List.of("6月", "六月"), List.of("销售额", "销售"), List.of("分类", "品类")),
            """
            SELECT p.category AS "商品分类",
                   SUM(oi.quantity * oi.unit_price) AS "销售额"
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            JOIN products p ON p.id = oi.product_id
            WHERE o.created_at >= '2026-06-01' AND o.created_at < '2026-07-01'
              AND o.status <> 'REFUNDED'
            GROUP BY p.category
            ORDER BY "销售额" DESC
            LIMIT 5"""
        ),
        // 2. 高价值客户：客户 + 消费/累计
        new GoldenQuery(
            "累计消费TOP10客户",
            List.of(List.of("客户"), List.of("消费", "累计")),
            """
            SELECT c.id AS "客户ID",
                   c.name AS "客户姓名",
                   c.level AS "客户等级",
                   SUM(o.total_amount) AS "累计消费金额"
            FROM customers c
            JOIN orders o ON o.customer_id = c.id
            WHERE o.status <> 'REFUNDED'
            GROUP BY c.id, c.name, c.level
            ORDER BY "累计消费金额" DESC
            LIMIT 10"""
        ),
        // 3. 支付渠道占比：支付/渠道 + 占比/6月
        new GoldenQuery(
            "6月支付渠道金额占比",
            List.of(List.of("支付", "渠道"), List.of("占比", "6月", "六月")),
            """
            SELECT p.channel AS "支付渠道",
                   SUM(p.amount) AS "支付金额",
                   ROUND(SUM(p.amount) * 100.0 / SUM(SUM(p.amount)) OVER (), 2) AS "金额占比(%)"
            FROM payments p
            JOIN orders o ON o.id = p.order_id
            WHERE o.created_at >= '2026-06-01' AND o.created_at < '2026-07-01'
              AND o.status <> 'REFUNDED'
            GROUP BY p.channel
            ORDER BY "支付金额" DESC
            LIMIT 10"""
        ),
        // 4. 分类客单价：客单价 + 分类
        new GoldenQuery(
            "各分类平均客单价",
            List.of(List.of("客单价"), List.of("分类", "品类")),
            """
            SELECT p.category AS "商品分类",
                   COUNT(DISTINCT o.id) AS "订单数",
                   ROUND(AVG(o.total_amount), 2) AS "平均客单价"
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            JOIN products p ON p.id = oi.product_id
            WHERE o.status <> 'REFUNDED'
            GROUP BY p.category
            ORDER BY "平均客单价" DESC
            LIMIT 10"""
        )
    );

    /**
     * 按关键词匹配黄金问题：命中返回内置 SQL，否则返回 CANNOT_ANSWER。
     * schemaContext 参数在规则版里用不上（SQL 是内置的），保留是为了和真实模型同一接口。
     */
    @Override
    public String generateSql(String question, String schemaContext) {
        if (question == null || question.isBlank()) {
            return CANNOT_ANSWER;
        }
        return GOLDEN_QUERIES.stream()
            .filter(query -> query.matches(question))
            .findFirst()
            .map(GoldenQuery::sql)
            .orElse(CANNOT_ANSWER);
    }

    /** 演示和文档里展示用的黄金问题清单 */
    public List<String> goldenQuestionNames() {
        return GOLDEN_QUERIES.stream().map(GoldenQuery::name).toList();
    }
}
