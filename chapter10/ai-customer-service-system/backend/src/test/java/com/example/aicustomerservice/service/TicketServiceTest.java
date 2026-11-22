package com.example.aicustomerservice.service;

import com.example.aicustomerservice.common.BusinessException;
import com.example.aicustomerservice.dao.InMemoryTicketDao;
import com.example.aicustomerservice.entity.CustomerTier;
import com.example.aicustomerservice.entity.IntentType;
import com.example.aicustomerservice.entity.SupportTicket;
import com.example.aicustomerservice.entity.TicketPriority;
import com.example.aicustomerservice.entity.TicketStatus;
import java.time.Clock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketServiceTest {

    @Test
    void shouldReuseTicketByIdempotencyKey() {
        Clock clock = Clock.systemUTC();
        TicketService ticketService = new TicketService(
            new InMemoryTicketDao(),
            new TicketIdGenerator(clock),
            clock
        );

        SupportTicket first = ticketService.createOrReuse(
            "C10001",
            CustomerTier.ENTERPRISE,
            "S1",
            "这个订单能退款吗？",
            "需要人工确认",
            IntentType.REFUND
        );
        SupportTicket second = ticketService.createOrReuse(
            "C10001",
            CustomerTier.ENTERPRISE,
            "S1",
            "这个订单能退款吗",
            "需要人工确认",
            IntentType.REFUND
        );

        assertThat(second.ticketId()).isEqualTo(first.ticketId());
        assertThat(second.duplicateCount()).isEqualTo(1);
        assertThat(second.priority()).isEqualTo(TicketPriority.URGENT);
    }

    @Test
    void shouldReopenResolvedTicketWhenDuplicateQuestionArrivesAgain() {
        Clock clock = Clock.systemUTC();
        TicketService ticketService = new TicketService(
            new InMemoryTicketDao(),
            new TicketIdGenerator(clock),
            clock
        );

        SupportTicket first = ticketService.createOrReuse(
            "C10001",
            CustomerTier.ENTERPRISE,
            "S1",
            "这个订单能退款吗？",
            "需要人工确认",
            IntentType.REFUND
        );
        ticketService.assign(first.ticketId(), "support-zhangsan");
        SupportTicket closed = ticketService.close(first.ticketId(), "support-zhangsan", "已处理完成");

        SupportTicket reopened = ticketService.createOrReuse(
            "C10001",
            CustomerTier.ENTERPRISE,
            "S2",
            "这个订单能退款吗",
            "客户再次咨询同一个问题",
            IntentType.REFUND
        );

        assertThat(closed.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(reopened.ticketId()).isEqualTo(first.ticketId());
        assertThat(reopened.status()).isEqualTo(TicketStatus.NEW);
        assertThat(reopened.resolvedAt()).isNull();
        assertThat(reopened.owner()).isBlank();
        assertThat(reopened.timeline()).anyMatch(event -> event.type().equals("REOPENED_BY_DUPLICATE"));
    }

    @Test
    void shouldRejectNoteAssignAndCloseAfterTicketIsResolved() {
        Clock clock = Clock.systemUTC();
        TicketService ticketService = new TicketService(
            new InMemoryTicketDao(),
            new TicketIdGenerator(clock),
            clock
        );

        SupportTicket ticket = ticketService.createOrReuse(
            "C10002",
            CustomerTier.VIP,
            "S-readonly",
            "物流三天没有更新，帮我催一下",
            "需要人工催办",
            IntentType.LOGISTICS_QUERY
        );
        // 未解决时可以正常追加内部备注。
        assertThat(ticketService.addNote(ticket.ticketId(), "support-zhangsan", "已联系承运商").timeline())
            .anyMatch(event -> event.type().equals("NOTE_ADDED"));
        ticketService.close(ticket.ticketId(), "support-zhangsan", "承运商已更新轨迹");

        // 解决后工单只读：备注、分配、再次解决都返回同一个业务错误码，时间线不再变化。
        int timelineSize = ticketService.get(ticket.ticketId()).timeline().size();
        assertThatThrownBy(() -> ticketService.addNote(ticket.ticketId(), "support-zhangsan", "解决后的补充"))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.code()).isEqualTo("TICKET_ALREADY_RESOLVED"));
        assertThatThrownBy(() -> ticketService.assign(ticket.ticketId(), "support-lisi"))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.code()).isEqualTo("TICKET_ALREADY_RESOLVED"));
        assertThatThrownBy(() -> ticketService.close(ticket.ticketId(), "support-zhangsan", "重复关闭"))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.code()).isEqualTo("TICKET_ALREADY_RESOLVED"));
        assertThat(ticketService.get(ticket.ticketId()).timeline()).hasSize(timelineSize);
    }
}
