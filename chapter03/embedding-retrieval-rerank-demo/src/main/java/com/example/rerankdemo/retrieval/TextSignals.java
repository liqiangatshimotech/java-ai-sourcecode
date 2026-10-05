package com.example.rerankdemo.retrieval;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 文本信号工具。
 *
 * <p>这里不用复杂分词器，是为了让 demo 保持可读。生产环境可以换成 Lucene / Elasticsearch
 * 的 analyzer，也可以接入业务词典。
 */
public final class TextSignals {

    private TextSignals() {
    }

    public static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    public static boolean containsAny(String text, List<String> phrases) {
        String normalized = normalize(text);
        return phrases.stream().anyMatch(normalized::contains);
    }

    public static int countPhraseMatches(String text, List<String> phrases) {
        String normalized = normalize(text);
        int count = 0;
        for (String phrase : phrases) {
            if (normalized.contains(phrase)) {
                count++;
            }
        }
        return count;
    }

    public static List<String> roughTerms(String query) {
        return Arrays.stream(normalize(query).split("[\\s,，.。?？!！:：;；]+"))
            .map(String::trim)
            .filter(term -> !term.isBlank())
            .toList();
    }
}
