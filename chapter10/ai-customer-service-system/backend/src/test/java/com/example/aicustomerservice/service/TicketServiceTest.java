package com.example.aicustomerservice.service;

import com.example.aicustomerservice.dao.InMemoryTicketDao;
import com.example.aicustomerservice.entity.CustomerTier;
import com.example.aicustomerservice.entity.IntentType;
import com.example.aicustomerservice.entity.SupportTicket;
import com.example.aicustomerservice.entity.TicketPriority;
import com.example.aicustomerservice.entity.TicketStatus;
import java.time.Clock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
