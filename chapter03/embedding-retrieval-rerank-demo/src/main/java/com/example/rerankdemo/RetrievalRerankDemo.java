package com.example.rerankdemo;

import com.example.rerankdemo.config.CourseChatSettings;
import com.example.rerankdemo.model.DemoCorpus;
import com.example.rerankdemo.model.KnowledgeChunk;
import com.example.rerankdemo.model.SearchHit;
import com.example.rerankdemo.rerank.BusinessReranker;
import com.example.rerankdemo.retrieval.ConceptVectorizer;
import com.example.rerankdemo.retrieval.HybridRetriever;
import com.example.rerankdemo.retrieval.KeywordRetriever;
import com.example.rerankdemo.retrieval.QueryExpander;
import com.example.rerankdemo.retrieval.VectorRetriever;

import java.util.List;

/**
 * 命令行入口。
 *
 * <p>默认问题会触发一个常见线上现象：一阶段向量召回 Top1 看起来相似，但业务上答错。
 * 解决办法不是只调 Prompt，而是扩大候选池、做查询扩展、混合召回，再用重排选最终上下文。
 */
public final class RetrievalRerankDemo {

    private static final String DEFAULT_QUERY = "报销凭证公司名称不对，能不能重新开？";

    private RetrievalRerankDemo() {
    }

    public static void main(String[] args) {
        String query = args.length == 0 ? DEFAULT_QUERY : String.join(" ", args);
        String tenantId = "acme";

        List<KnowledgeChunk> chunks = DemoCorpus.supportChunks();
        ConceptVectorizer vectorizer = new ConceptVectorizer();
        VectorRetriever vectorRetriever = new VectorRetriever(chunks, vectorizer);
        KeywordRetriever keywordRetriever = new KeywordRetriever(chunks);
        HybridRetriever hybridRetriever = new HybridRetriever(vectorRetriever, keywordRetriever);
        BusinessReranker reranker = new BusinessReranker();
        QueryExpander queryExpander = new QueryExpander();

        CourseChatSettings chatSettings = CourseChatSettings.fromEnvironment();
        System.out.println("问题：" + query);
        System.out.println("Chat 模型配置：" + chatSettings.safeSummary() + "（本 demo 不调用 Chat 模型）");
        System.out.println();

        List<SearchHit> vectorTop1 = vectorRetriever.search(query, tenantId, 1);
        print("只取向量 Top1：", vectorTop1);

        List<SearchHit> vectorPool = vectorRetriever.search(query, tenantId, 3);
        print("扩大向量候选池 Top3：", vectorPool);

        List<SearchHit> reranked = reranker.rerank(query, vectorPool, 3);
        print("对 Top3 候选重排：", reranked);

        String expandedQuery = queryExpander.expand(query);
        List<SearchHit> expandedHybrid = hybridRetriever.search(expandedQuery, tenantId, 3);
        print("查询扩展 + 混合召回：", expandedHybrid);
    }

    private static void print(String title, List<SearchHit> hits) {
        System.out.println(title);
        for (int i = 0; i < hits.size(); i++) {
            SearchHit hit = hits.get(i);
            System.out.printf(
                "%d. %s | %s | score=%.4f | %s%n",
                i + 1,
                hit.chunk().id(),
                hit.chunk().title(),
                hit.score(),
                hit.reason()
            );
        }
        System.out.println();
    }
}
