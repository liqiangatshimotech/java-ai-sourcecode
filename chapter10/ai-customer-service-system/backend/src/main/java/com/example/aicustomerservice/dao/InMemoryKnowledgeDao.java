package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.KnowledgeDocument;
import com.example.aicustomerservice.entity.KnowledgeStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 知识库内存实现。
 *
 * 内存模式保留版本号、启停状态和更新时间，让上层流程不依赖具体存储。
 */
public class InMemoryKnowledgeDao implements KnowledgeDao {

    private final Map<String, KnowledgeDocument> documents = new ConcurrentHashMap<>();

    public InMemoryKnowledgeDao(Clock clock) {
        Instant now = clock.instant();
        save(new KnowledgeDocument(
            "KB-REFUND-001",
            "v2026.06",
            "退款与售后规则",
            """
                普通订单在签收后 7 天内可以申请无理由退款，生鲜、定制和虚拟商品除外。
                VIP 客户的退款审核 SLA 为 2 小时，企业客户可以走客户成功专属通道。
                如果订单已经进入仓库拣货完成状态，系统只能先创建售后工单，由人工确认是否拦截发货。
                """,
            KnowledgeStatus.PUBLISHED,
            "BUILT_IN",
            "course-seed",
            "system",
            now,
            now
        ));
        save(new KnowledgeDocument(
            "KB-SHIPPING-001",
            "v2026.06",
            "物流查询规则",
            """
                物流状态以承运商回传为准。超过 24 小时没有更新时，先查询运单轨迹，再判断是否创建物流催办工单。
                高价值企业订单需要同步客户成功经理，避免只给出模板化回复。
                """,
            KnowledgeStatus.PUBLISHED,
            "BUILT_IN",
            "course-seed",
            "system",
            now,
            now
        ));
        save(new KnowledgeDocument(
            "KB-INVOICE-001",
            "v2026.06",
            "发票处理规则",
            """
                电子发票通常在支付完成后 10 分钟内生成。抬头错误可以在开票后 24 小时内申请重开。
                企业客户需要专票时，必须校验税号、开户行和注册地址。
                """,
            KnowledgeStatus.PUBLISHED,
            "BUILT_IN",
            "course-seed",
            "system",
            now,
            now
        ));
        save(new KnowledgeDocument(
            "KB-AI-GUARDRAIL-001",
            "v2026.06",
            "客服回复安全边界",
            """
                回复必须优先依据知识库和业务系统查询结果。不确定时不要编造政策、不要承诺赔付金额，
                应该创建工单并说明需要人工确认。
                """,
            KnowledgeStatus.PUBLISHED,
            "BUILT_IN",
            "course-seed",
            "system",
            now,
            now
        ));
    }

    @Override
    public List<KnowledgeDocument> findPublishedDocuments() {
        return documents.values()
            .stream()
            .filter(KnowledgeDocument::published)
            .sorted(Comparator.comparing(KnowledgeDocument::updatedAt).reversed())
            .toList();
    }

    @Override
    public List<KnowledgeDocument> search(String keyword, KnowledgeStatus status) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        return documents.values()
            .stream()
            .filter(document -> status == null || document.status() == status)
            .filter(document -> normalizedKeyword.isBlank()
                || document.title().toLowerCase().contains(normalizedKeyword)
                || document.content().toLowerCase().contains(normalizedKeyword)
                || document.documentId().toLowerCase().contains(normalizedKeyword))
            .sorted(Comparator.comparing(KnowledgeDocument::updatedAt).reversed())
            .toList();
    }

    @Override
    public Optional<KnowledgeDocument> findByDocumentId(String documentId) {
        return Optional.ofNullable(documents.get(documentId));
    }

    @Override
    public KnowledgeDocument save(KnowledgeDocument document) {
        documents.put(document.documentId(), document);
        return document;
    }

    public List<KnowledgeDocument> allDocuments() {
        return new ArrayList<>(documents.values());
    }
}
