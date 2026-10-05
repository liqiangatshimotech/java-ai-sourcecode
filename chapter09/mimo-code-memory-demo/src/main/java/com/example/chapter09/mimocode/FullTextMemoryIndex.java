package com.example.chapter09.mimocode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 一个很小的全文索引，用来模拟 SQLite FTS5 的核心价值：把散落在磁盘上的
 * Markdown 记忆召回给 Agent。真实项目可以替换成 SQLite FTS5、Lucene、
 * Elasticsearch 或数据库自带全文索引。
 */
public final class FullTextMemoryIndex {
    private final Map<String, MemoryDocument> documents = new LinkedHashMap<>();

    public void add(MemoryDocument document) {
        documents.put(document.id(), document);
    }

    public List<SearchHit> search(String query, int limit) {
        List<String> terms = tokenize(query);
        if (terms.isEmpty()) {
            return List.of();
        }

        return documents.values().stream()
                .map(document -> score(document, terms))
                .filter(hit -> hit.score() > 0)
                .sorted(Comparator.comparingDouble(SearchHit::score).reversed())
                .limit(limit)
                .toList();
    }

    private SearchHit score(MemoryDocument document, List<String> terms) {
        String body = document.body().toLowerCase(Locale.ROOT);
        double score = 0;
        for (String term : terms) {
            int count = countTerm(body, term);
            if (count > 0) {
                // 这里不是完整 BM25，只是用“命中词数量 + 词频”模拟相关性评分。
                score += 1.0 + Math.log(1 + count);
            }
        }
        return new SearchHit(document, score, snippet(document.body(), terms));
    }

    private static List<String> tokenize(String value) {
        String[] raw = value.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}_\\-]+");
        List<String> terms = new ArrayList<>();
        for (String term : raw) {
            if (!term.isBlank()) {
                terms.add(term);
            }
        }
        return terms;
    }

    private static int countTerm(String body, String term) {
        int count = 0;
        int index = 0;
        while ((index = body.indexOf(term, index)) >= 0) {
            count++;
            index += term.length();
        }
        return count;
    }

    private static String snippet(String body, List<String> terms) {
        String lower = body.toLowerCase(Locale.ROOT);
        int hit = lower.length();
        for (String term : terms) {
            int index = lower.indexOf(term);
            if (index >= 0) {
                hit = Math.min(hit, index);
            }
        }
        if (hit == lower.length()) {
            return body.length() <= 120 ? body : body.substring(0, 120) + "...";
        }
        int start = Math.max(0, hit - 40);
        int end = Math.min(body.length(), hit + 120);
        return (start > 0 ? "..." : "") + body.substring(start, end).replace('\n', ' ') + (end < body.length() ? "..." : "");
    }

    public record SearchHit(MemoryDocument document, double score, String snippet) {
    }
}
