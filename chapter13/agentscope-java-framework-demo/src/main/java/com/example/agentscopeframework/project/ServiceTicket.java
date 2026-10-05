package com.example.agentscopeframework.project;

/**
 * 客服工单输入。
 *
 * <p>这个 record 模拟后端从客服系统、IM 系统或工单平台拿到的业务对象。把业务字段结构化之后，Agent 的 Prompt
 * 就不需要从一大段自然语言里猜测订单号、用户等级和问题内容。
 */
public record ServiceTicket(
        String ticketId,
        String userLevel,
        String channel,
        String title,
        String description,
        TicketPriority priority) {}
