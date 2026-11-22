package com.example.aicustomerservice.service;

import com.example.aicustomerservice.common.BusinessException;
import com.example.aicustomerservice.common.Hashes;
import com.example.aicustomerservice.dao.TicketDao;
import com.example.aicustomerservice.dao.TicketQuery;
import com.example.aicustomerservice.entity.CustomerTier;
import com.example.aicustomerservice.entity.IntentType;
import com.example.aicustomerservice.entity.SupportTicket;
import com.example.aicustomerservice.entity.TicketDashboard;
import com.example.aicustomerservice.entity.TicketPriority;
import com.example.aicustomerservice.entity.TicketSlaSnapshot;
import com.example.aicustomerservice.entity.TicketStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * 工单工作流服务。
 *
 * 这里处理生产系统里非常关键的幂等逻辑：同一个客户短时间重复提问时，
 * 复用已有工单，而不是不断制造重复待办。
 */
public class TicketService {

    private final TicketDao ticketDao;
    private final TicketIdGenerator ticketIdGenerator;
    private final TicketSlaPolicy slaPolicy;
    private final Clock clock;

    public TicketService(TicketDao ticketDao, TicketIdGenerator ticketIdGenerator, Clock clock) {
        this.ticketDao = ticketDao;
        this.ticketIdGenerator = ticketIdGenerator;
        this.slaPolicy = new TicketSlaPolicy();
        this.clock = clock;
    }

    public SupportTicket createOrReuse(
        String customerId,
        CustomerTier customerTier,
        String sessionId,
        String question,
        String answerDraft,
        IntentType intent
    ) {
        // Clock 由 Spring 注入，测试时可以换成固定时间，避免时间断言抖动。
        Instant now = clock.instant();

        // 先转小写、去掉空白和常见标点，再对问题生成摘要。
        // 同一客户“这个订单能退款吗？”和“这个订单能退款吗”会得到同一个键。
        String idempotencyKey = idempotencyKey(customerId, question);

        return ticketDao.findByIdempotencyKey(idempotencyKey)
            // 已存在时不再创建新工单，而是增加重复次数并写入时间线。
            .map(existing -> ticketDao.save(existing.markDuplicate(now)))
            // 没有命中时才生成新工单号，客户等级和意图共同决定优先级。
            .orElseGet(() -> ticketDao.save(SupportTicket.create(
                ticketIdGenerator.nextTicketId(),
                customerId,
                sessionId,
                question,
                answerDraft,
                intent.name(),
                priorityFor(customerTier, intent),
                idempotencyKey,
                now
            )));
    }

    public SupportTicket get(String ticketId) {
        return ticketDao.findByTicketId(ticketId)
            .orElseThrow(() -> new BusinessException("TICKET_NOT_FOUND", "工单不存在：" + ticketId));
    }

    public List<SupportTicket> search(TicketStatus status, String customerId) {
        return ticketDao.search(new TicketQuery(status, customerId));
    }

    public TicketDashboard dashboard() {
        Instant now = clock.instant();
        List<SupportTicket> tickets = search(null, null);
        int newCount = count(tickets, TicketStatus.NEW);
        int assignedCount = count(tickets, TicketStatus.ASSIGNED);
        int waitingCustomerCount = count(tickets, TicketStatus.WAITING_CUSTOMER);
        int resolvedCount = count(tickets, TicketStatus.RESOLVED);
        int urgentCount = (int) tickets.stream()
            .filter(ticket -> ticket.priority() == TicketPriority.URGENT)
            .count();
        int breachedCount = (int) tickets.stream()
            .map(ticket -> slaPolicy.evaluate(ticket, now))
            .filter(TicketSlaSnapshot::breached)
            .count();
        int dueSoonCount = (int) tickets.stream()
            .map(ticket -> slaPolicy.evaluate(ticket, now))
            .filter(sla -> "DUE_SOON".equals(sla.status()))
            .count();
        int duplicateHitCount = tickets.stream()
            .mapToInt(SupportTicket::duplicateCount)
            .sum();

        return new TicketDashboard(
            tickets.size(),
            newCount + assignedCount + waitingCustomerCount,
            newCount,
            assignedCount,
            resolvedCount,
            urgentCount,
            breachedCount,
            dueSoonCount,
            duplicateHitCount,
            now
        );
    }

    public SupportTicket assign(String ticketId, String owner) {
        SupportTicket ticket = get(ticketId);
        if (ticket.status() == TicketStatus.RESOLVED) {
            throw new BusinessException("TICKET_ALREADY_RESOLVED", "已关闭工单不能重新分配：" + ticketId);
        }
        return ticketDao.save(ticket.assign(owner.trim(), clock.instant()));
    }

    public SupportTicket addNote(String ticketId, String operator, String message) {
        SupportTicket ticket = get(ticketId);
        // 已解决工单只读：和分配、关闭保持同一条规则，避免结论写完后时间线还在被追加。
        // 需要继续处理时，由重复问题命中幂等键重新打开原工单。
        if (ticket.status() == TicketStatus.RESOLVED) {
            throw new BusinessException("TICKET_ALREADY_RESOLVED", "已关闭工单不能再添加备注：" + ticketId);
        }
        return ticketDao.save(ticket.addNote(operator.trim(), message.trim(), clock.instant()));
    }

    public SupportTicket close(String ticketId, String operator, String summary) {
        SupportTicket ticket = get(ticketId);
        if (ticket.status() == TicketStatus.RESOLVED) {
            throw new BusinessException("TICKET_ALREADY_RESOLVED", "工单已经关闭：" + ticketId);
        }
        return ticketDao.save(ticket.close(operator.trim(), summary.trim(), clock.instant()));
    }

    public String idempotencyKey(String customerId, String question) {
        String normalizedQuestion = question.toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", "")
            .replaceAll("[，。！？；：、,.!?;:]", "");
        return customerId + ":" + Hashes.sha256Short(normalizedQuestion);
    }

    private TicketPriority priorityFor(CustomerTier tier, IntentType intent) {
        if (tier == CustomerTier.ENTERPRISE) {
            return TicketPriority.URGENT;
        }
        if (tier == CustomerTier.VIP || intent == IntentType.REFUND) {
            return TicketPriority.HIGH;
        }
        return TicketPriority.NORMAL;
    }

    private int count(List<SupportTicket> tickets, TicketStatus status) {
        return (int) tickets.stream()
            .filter(ticket -> ticket.status() == status)
            .count();
    }
}
