package com.example.existingservicemcp.existing;

import com.example.existingservicemcp.security.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class SupportTicketService {

    private final TicketRepository ticketRepository;

    public SupportTicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /*
     * 这是改造前就已经存在的业务能力。
     * 它不知道 MCP 的存在，只返回适合对外展示的 TicketView，避免泄露内部备注。
     */
    public TicketView getTicket(CurrentUser user, String ticketId) {
        SupportTicket ticket = loadAllowedTicket(user, ticketId);
        return new TicketView(
            ticket.ticketId(),
            ticket.customerName(),
            ticket.customerTier(),
            ticket.channel(),
            ticket.subject(),
            ticket.status(),
            "%s：%s".formatted(ticket.subject(), ticket.content())
        );
    }

    /*
     * 回复草稿规则仍然放在业务层，HTTP Controller 和 MCP Tool 都可以复用。
     * 企业客户、发票和生产故障场景会被标记为需要人工审批。
     */
    public ReplyDraft draftReply(CurrentUser user, String ticketId, String tone) {
        SupportTicket ticket = loadAllowedTicket(user, ticketId);
        boolean needsApproval = ticket.customerTier() == CustomerTier.ENTERPRISE
            || ticket.content().contains("发票")
            || ticket.content().contains("生产");

        String reply = switch (tone) {
            case "formal" -> "您好，已收到您的问题。我们会优先核对相关记录，并在确认后给出处理方案。";
            case "warm" -> "您好，非常理解这个问题对您当前工作的影响。我们会马上核对并尽快给您明确回复。";
            default -> "您好，已收到问题，我们会尽快处理。";
        };

        return new ReplyDraft(ticket.ticketId(), tone, reply, needsApproval);
    }

    public String slaPolicy(CurrentUser user) {
        return """
            # ACME 客服 SLA
            - Enterprise 客户：15 分钟内响应，必要时升级客户成功经理。
            - Pro 客户：2 小时内响应。
            - Free 客户：1 个工作日内响应。
            - 涉及发票、生产故障、退款承诺的回复必须人工审批。
            """;
    }

    // 所有入口共享同一段权限校验，避免 MCP 旁路绕过原系统的安全边界。
    private SupportTicket loadAllowedTicket(CurrentUser user, String ticketId) {
        if (!user.hasRole("support_agent")) {
            throw new SecurityException("当前用户缺少 support_agent 角色。");
        }

        SupportTicket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new IllegalArgumentException("工单不存在：" + ticketId));

        if (!ticket.tenantId().equals(user.tenantId())) {
            throw new SecurityException("当前用户无权读取该租户工单。");
        }
        return ticket;
    }
}
