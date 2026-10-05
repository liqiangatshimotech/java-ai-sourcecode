package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.SupportTicket;
import com.example.aicustomerservice.entity.TicketPriority;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTicketDao implements TicketDao {

    private final Map<String, SupportTicket> tickets = new ConcurrentHashMap<>();

    /**
     * 单元测试默认从空仓库开始，避免种子工单影响幂等断言。
     * 应用运行时由配置类调用带 Clock 的构造方法，加载可重复操作的真实工单场景。
     */
    public InMemoryTicketDao() {
    }

    public InMemoryTicketDao(Clock clock) {
        seed(clock.instant());
    }

    @Override
    public SupportTicket save(SupportTicket ticket) {
        tickets.put(ticket.ticketId(), ticket);
        return ticket;
    }

    @Override
    public Optional<SupportTicket> findByTicketId(String ticketId) {
        return Optional.ofNullable(tickets.get(ticketId));
    }

    @Override
    public Optional<SupportTicket> findByIdempotencyKey(String idempotencyKey) {
        return tickets.values()
            .stream()
            .filter(ticket -> ticket.idempotencyKey().equals(idempotencyKey))
            .findFirst();
    }

    @Override
    public List<SupportTicket> search(TicketQuery query) {
        return tickets.values()
            .stream()
            .filter(ticket -> query.status() == null || ticket.status() == query.status())
            .filter(ticket -> query.customerId() == null || query.customerId().isBlank() || ticket.customerId().equals(query.customerId()))
            .sorted(Comparator.comparing(SupportTicket::createdAt).reversed())
            .limit(100)
            .toList();
    }

    private void seed(Instant now) {
        SupportTicket warehouseInterception = SupportTicket.create(
            "CS-20260710-0001",
            "C10001",
            "CONV-20260710-001",
            "高价值活动物料已经进入仓库，客户要求取消并退款。",
            "需要仓库确认是否拦截发货，再由财务确认退款路径。",
            "REFUND",
            TicketPriority.URGENT,
            "seed:warehouse-interception",
            now.minusSeconds(55 * 60)
        ).addNote("ai-orchestrator", "高价值订单触发人工审批护栏。", now.minusSeconds(54 * 60));

        SupportTicket logisticsDelay = SupportTicket.create(
            "CS-20260710-0002",
            "C10002",
            "CONV-20260710-002",
            "物流超过 24 小时没有更新，需要向承运商发起催查。",
            "建议先确认运单最新轨迹，再决定是否升级物流异常。",
            "LOGISTICS_QUERY",
            TicketPriority.HIGH,
            "seed:logistics-delay",
            now.minusSeconds(95 * 60)
        ).assign("support-zhangmin", now.minusSeconds(80 * 60));

        SupportTicket invoiceReview = SupportTicket.create(
            "CS-20260710-0003",
            "C10003",
            "CONV-20260710-003",
            "客户补交企业抬头和税号后，需要重新开具电子发票。",
            "等待客户提供完整开票资料。",
            "INVOICE",
            TicketPriority.NORMAL,
            "seed:invoice-review",
            now.minusSeconds(2 * 3600)
        ).assign("support-lijie", now.minusSeconds(110 * 60))
            .addNote("support-lijie", "已向客户索取税号与企业抬头。", now.minusSeconds(70 * 60));

        SupportTicket resolvedFaq = SupportTicket.create(
            "CS-20260710-0004",
            "C10003",
            "CONV-20260710-005",
            "会员积分未到账。",
            "核对积分流水后补发。",
            "FAQ",
            TicketPriority.LOW,
            "seed:points-reissue",
            now.minusSeconds(23 * 3600)
        ).assign("support-lijie", now.minusSeconds(22 * 3600))
            .close("support-lijie", "积分补发成功，客户已确认。", now.minusSeconds(20 * 3600));

        save(warehouseInterception);
        save(logisticsDelay);
        save(invoiceReview);
        save(resolvedFaq);
    }
}
