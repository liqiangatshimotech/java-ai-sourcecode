package com.example.aicustomerservice.service;

import com.example.aicustomerservice.dao.PgVectorKnowledgeVectorDao;
import com.example.aicustomerservice.entity.KnowledgeDocument;
import com.example.aicustomerservice.entity.ProductionIssueImplementation;
import com.example.aicustomerservice.entity.RetrievedKnowledge;
import java.util.List;

public class EnterpriseProblemCatalog {

    public List<ProductionIssueImplementation> list() {
        return List.of(
            new ProductionIssueImplementation(
                "8.1",
                "知识库召回不准",
                "语义切分、pgvector 向量召回、召回评分和低分触发转人工。",
                "rag/SemanticChunker.java, rag/PgVectorKnowledgeVectorDao.java, rag/RetrievalService.java, production/ProductionGuardrailService.java"
            ),
            new ProductionIssueImplementation(
                "8.2",
                "AI 胡说",
                "没有知识证据且没有工具结果时，不允许模型自由发挥，必须转人工创建工单。",
                "production/ProductionGuardrailService.java, supportService/CustomerSupportService.java"
            ),
            new ProductionIssueImplementation(
                "8.3",
                "上下文过长",
                "ConversationMemoryService 只保留滚动摘要窗口，不把全部历史消息塞进模型。",
                "supportService/ConversationMemoryService.java"
            ),
            new ProductionIssueImplementation(
                "8.4",
                "高并发延迟",
                "模型调用通过 QueuedModelExecutor 统一入口，FAQ 热点答案写入 HotQuestionCache。",
                "production/QueuedModelExecutor.java, production/InMemoryHotQuestionCache.java"
            ),
            new ProductionIssueImplementation(
                "8.5",
                "知识库更新不一致",
                "知识文档和召回结果带 version，接口返回 sources 时可追溯引用版本。",
                "rag/KnowledgeDocument.java, rag/RetrievedKnowledge.java"
            ),
            new ProductionIssueImplementation(
                "8.6",
                "工单重复生成",
                "TicketService 用 customerId + 归一化问题哈希生成幂等键，重复问题复用已有工单。",
                "ticket/TicketService.java"
            )
        );
    }
}
