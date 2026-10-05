package com.example.agentscope2.project;

/**
 * 结构化输出示例对象。
 *
 * <p>AgentScope 2.0 会优先使用模型原生 JSON Schema 能力；不支持时再退回工具选择方案。
 */
public record TicketDecision(
        String ticketId,
        String priority,
        String ownerTeam,
        String reason,
        String nextAction) {}
