package com.example.texttosqlagent.schema;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Schema Linking：从自然语言问题里找出「这个问题需要查哪几张表」。
 *
 * 为什么需要这一步？电商库可能有几十上百张表，把整个 schema 塞给模型既贵又容易误导。
 * 先圈定相关表，只把这几张表的 DDL 喂给模型，生成 SQL 的准确率会高得多。
 *
 * 打分策略（教学版，够用且透明）：
 * - 表注释、列注释按标点切成中文词组，问题里出现哪个词组就给那张表记一分；
 * - 命中业务指标词典的关键词时，给指标涉及的所有表记分；
 * - 得分大于 0 的表入选，按得分从高到低排序。
 *
 * 注释里说明一下：真实系统这一层通常用模型或向量检索做语义理解，
 * 这里用透明的规则实现，方便课堂上演示「选表选对了没有」。
 */
public class SchemaLinkingService {

    /** 注释文本里的噪声词，不参与匹配 */
    private static final Set<String> STOP_WORDS = Set.of("取值");

    private final List<TableSchema> tables;
    private final MetricDictionary metricDictionary;

    public SchemaLinkingService(List<TableSchema> tables, MetricDictionary metricDictionary) {
        this.tables = tables;
        this.metricDictionary = metricDictionary;
    }

    /**
     * 对一个问题做选表。
     *
     * @return 选中的表、每张表命中的关键词、以及拼好的 schema 上下文；
     *         一张都没选中时 selectedTables 为空，由上层决定诚实拒答而不是硬编 SQL
     */
    public SchemaLinkingResult link(String question) {
        Map<String, List<String>> matchedKeywords = new LinkedHashMap<>();
        List<ScoredTable> scored = new ArrayList<>();

        for (TableSchema table : tables) {
            // 候选词 = 表注释词组 + 列注释词组 + 指标词典里涉及本表的关键词
            Set<String> candidates = new LinkedHashSet<>();
            candidates.addAll(splitComment(table.comment()));
            for (ColumnSchema column : table.columns()) {
                candidates.addAll(splitComment(column.comment()));
            }
            for (MetricDictionary.MetricDefinition metric : metricDictionary.all()) {
                boolean involvesTable = metric.requiredTables().stream()
                    .anyMatch(required -> required.equalsIgnoreCase(table.name()));
                if (involvesTable) {
                    candidates.addAll(metric.keywords());
                }
            }

            // 子串匹配：问题里出现了候选词就算命中，命中几个记几分
            List<String> hits = candidates.stream()
                .filter(keyword -> keyword.length() >= 2 && question.contains(keyword))
                .distinct()
                .toList();
            if (!hits.isEmpty()) {
                scored.add(new ScoredTable(table, hits.size()));
                matchedKeywords.put(table.name(), hits);
            }
        }

        // 得分高的排前面；同分按表名排序，保证输出稳定可测试
        scored.sort(Comparator.comparingInt(ScoredTable::score).reversed()
            .thenComparing(scoredTable -> scoredTable.table().name()));
        List<TableSchema> selected = scored.stream().map(ScoredTable::table).toList();

        return new SchemaLinkingResult(selected, matchedKeywords, buildSchemaContext(question, selected));
    }

    /**
     * 把中文注释按标点切成词组。
     * 例如「商品分类，取值：数码/家电/服饰/食品」切成 商品分类 / 数码 / 家电 / 服饰 / 食品。
     * 中文没有空格分词，这是最朴素的「按词组匹配」做法。
     */
    private List<String> splitComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return List.of();
        }
        List<String> words = new ArrayList<>();
        for (String word : comment.split("[，。：:；;、/\\s()（）]+")) {
            String trimmed = word.trim();
            if (trimmed.length() >= 2 && !STOP_WORDS.contains(trimmed)) {
                words.add(trimmed);
            }
        }
        return words;
    }

    /**
     * 拼给模型看的 schema 上下文：选中表的 DDL 片段 + 问题命中的业务指标口径。
     */
    private String buildSchemaContext(String question, List<TableSchema> selected) {
        if (selected.isEmpty()) {
            return "";
        }
        StringBuilder context = new StringBuilder("【数据库表结构】\n");
        for (TableSchema table : selected) {
            context.append(table.toDdlFragment()).append("\n\n");
        }
        List<MetricDictionary.MetricDefinition> matchedMetrics = metricDictionary.findMatching(question);
        if (!matchedMetrics.isEmpty()) {
            context.append("【业务指标口径】\n");
            for (MetricDictionary.MetricDefinition metric : matchedMetrics) {
                context.append("- ").append(metric.name())
                    .append("：").append(metric.definition())
                    .append("；SQL 表达式：").append(metric.sqlExpression())
                    .append("\n");
            }
        }
        return context.toString();
    }

    /** 打分用的内部临时结构：一张表和它的命中次数 */
    private record ScoredTable(TableSchema table, int score) {
    }
}
