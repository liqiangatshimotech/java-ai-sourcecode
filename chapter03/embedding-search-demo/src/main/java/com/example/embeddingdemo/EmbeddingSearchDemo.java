package com.example.embeddingdemo;

import com.example.embeddingdemo.config.CourseChatSettings;
import com.example.embeddingdemo.embedding.CourseEmbeddingModels;
import com.example.embeddingdemo.model.KnowledgeEntry;
import com.example.embeddingdemo.model.SearchResult;
import com.example.embeddingdemo.search.DemoCorpus;
import com.example.embeddingdemo.search.EmbeddingSearchEngine;
import com.example.embeddingdemo.search.KeywordSearchEngine;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.List;

/**
 * 命令行入口。
 *
 * <p>默认查询故意不包含“发票”两个字，而是说“报销凭证公司名称不对”。
 * 运行结果可以直观看到：关键词检索只看字面，Embedding 检索看语义方向。
 */
public final class EmbeddingSearchDemo {

    private static final String DEFAULT_QUERY = "报销凭证公司名称不对，能不能重新开一份？";

    private EmbeddingSearchDemo() {
    }

    public static void main(String[] args) {
        String query = args.length == 0 ? DEFAULT_QUERY : String.join(" ", args);
        List<KnowledgeEntry> entries = DemoCorpus.supportKnowledge();

        CourseChatSettings chatSettings = CourseChatSettings.fromEnvironment();
        EmbeddingModel embeddingModel = CourseEmbeddingModels.fromEnvironment();

        System.out.println("问题：" + query);
        System.out.println("Chat 模型配置：" + chatSettings.safeSummary() + "（本 demo 不调用 Chat 模型）");
        System.out.println("Embedding 模型：" + embeddingModel.modelName());
        System.out.println();

        KeywordSearchEngine keywordSearch = new KeywordSearchEngine(entries);
        System.out.println("关键词检索 Top3：");
        print(keywordSearch.search(query, 3));
        System.out.println();

        EmbeddingSearchEngine embeddingSearch = EmbeddingSearchEngine.index(entries, embeddingModel);
        System.out.println("Embedding 语义检索 Top3：");
        print(embeddingSearch.search(query, 3));
    }

    private static void print(List<SearchResult> results) {
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            System.out.printf(
                "%d. %s | %s | score=%.4f%n",
                i + 1,
                result.entry().id(),
                result.entry().title(),
                result.score()
            );
        }
    }
}
