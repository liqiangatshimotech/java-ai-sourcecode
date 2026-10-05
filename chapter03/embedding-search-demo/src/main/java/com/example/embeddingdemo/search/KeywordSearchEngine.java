package com.example.embeddingdemo.search;

import com.example.embeddingdemo.model.KnowledgeEntry;
import com.example.embeddingdemo.model.SearchResult;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 一个故意保持朴素的关键词检索。
 *
 * <p>它只做分词后的 contains 匹配，不理解“报销凭证”和“发票”、“重新开”和“重开”的关系。
 * 这个类的存在不是为了做一个好搜索引擎，而是为了制造一个清晰的对照组。
 */
public final class KeywordSearchEngine {

    private final List<KnowledgeEntry> entries;

    public KeywordSearchEngine(List<KnowledgeEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    public List<SearchResult> search(String query, int topK) {
        List<String> terms = splitTerms(query);

        return entries.stream()
            .map(entry -> new SearchResult(entry, score(entry, terms)))
            .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
            .limit(Math.max(1, topK))
            .toList();
    }

    private double score(KnowledgeEntry entry, List<String> terms) {
        if (terms.isEmpty()) {
            return 0.0;
        }

        String searchableText = normalize(entry.searchableText());
        long matchedTerms = terms.stream()
            .filter(searchableText::contains)
            .count();

        return (double) matchedTerms / terms.size();
    }

    private List<String> splitTerms(String query) {
        return Arrays.stream(normalize(query).split("[\\s,，.。?？!！:：;；]+"))
            .map(String::trim)
            .filter(term -> !term.isBlank())
            .toList();
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}
