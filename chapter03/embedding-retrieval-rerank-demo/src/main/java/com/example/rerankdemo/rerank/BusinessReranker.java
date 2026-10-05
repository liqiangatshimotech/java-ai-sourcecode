package com.example.rerankdemo.rerank;

import com.example.rerankdemo.model.SearchHit;
import com.example.rerankdemo.retrieval.TextSignals;

import java.util.Comparator;
import java.util.List;

/**
 * 一个规则版重排器，用来模拟 Cross-Encoder Reranker 的作用。
 *
 * <p>真实 reranker 会同时读取 query 和 candidate 文本，直接判断二者是否匹配。
 * 这里用业务规则模拟这个过程，便于在没有模型服务的环境里稳定运行测试。
 */
public final class BusinessReranker {

    private static final List<String> INVOICE_QUERY_TERMS = List.of("报销凭证", "发票", "开票", "票据");
    private static final List<String> INVOICE_DOC_TERMS = List.of("发票", "开票", "票据", "抬头");
    private static final List<String> CORRECTION_TERMS = List.of("写错", "填错", "不对", "错误", "更正", "重开", "重新");
    private static final List<String> ADMIN_DISTRACTOR_TERMS = List.of("营业执照", "认证资料", "企业资料");

    public List<SearchHit> rerank(String query, List<SearchHit> candidates, int topK) {
        return candidates.stream()
            .map(candidate -> new SearchHit(
                candidate.chunk(),
                score(query, candidate),
                "rerank from " + candidate.reason()
            ))
            .sorted(Comparator.comparingDouble(SearchHit::score).reversed())
            .limit(Math.max(1, topK))
            .toList();
    }

    private double score(String query, SearchHit candidate) {
        String text = candidate.chunk().searchableText();
        double score = candidate.score() * 0.20;

        boolean invoiceQuestion = TextSignals.containsAny(query, INVOICE_QUERY_TERMS);
        if (invoiceQuestion && TextSignals.containsAny(text, INVOICE_DOC_TERMS)) {
            score += 0.65;
        }
        if (TextSignals.containsAny(query, CORRECTION_TERMS) && TextSignals.containsAny(text, CORRECTION_TERMS)) {
            score += 0.25;
        }
        if (invoiceQuestion && TextSignals.containsAny(text, ADMIN_DISTRACTOR_TERMS)) {
            score -= 0.45;
        }

        return score;
    }
}
