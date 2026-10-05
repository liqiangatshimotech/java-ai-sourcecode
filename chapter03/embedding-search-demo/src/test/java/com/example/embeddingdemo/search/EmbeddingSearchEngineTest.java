package com.example.embeddingdemo.search;

import com.example.embeddingdemo.embedding.LocalConceptEmbeddingModel;
import com.example.embeddingdemo.model.KnowledgeEntry;
import com.example.embeddingdemo.model.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingSearchEngineTest {

    @Test
    void semanticSearchCanFindInvoiceEntryWithoutLiteralInvoiceWord() {
        List<KnowledgeEntry> entries = DemoCorpus.supportKnowledge();
        String query = "报销凭证公司名称不对，能不能重新开一份？";

        KeywordSearchEngine keywordSearch = new KeywordSearchEngine(entries);
        SearchResult keywordTop1 = keywordSearch.search(query, 1).getFirst();

        EmbeddingSearchEngine semanticSearch = EmbeddingSearchEngine.index(entries, new LocalConceptEmbeddingModel());
        SearchResult semanticTop1 = semanticSearch.search(query, 1).getFirst();

        // 关键词检索看不到“报销凭证”和“发票”的关系，所以默认顺序里的退款条目会排在前面。
        assertNotEquals("KB-INVOICE-001", keywordTop1.entry().id());

        // 概念 Embedding 把“报销凭证、发票、票据”放到相近方向，能把发票问题召回。
        assertEquals("KB-INVOICE-001", semanticTop1.entry().id());
        assertTrue(semanticTop1.score() > 0.50);
    }
}
