package com.example.existingservicemcp.mcp;

import com.example.existingservicemcp.existing.ReplyDraft;
import com.example.existingservicemcp.existing.SupportTicketService;
import com.example.existingservicemcp.existing.TicketView;
import com.example.existingservicemcp.security.CurrentUser;
import com.example.existingservicemcp.security.DemoUserContext;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SupportTicketMcpAdapter {

    private final DemoUserContext userContext;
    private final SupportTicketService supportTicketService;
    private final McpAuditLog auditLog;

    public SupportTicketMcpAdapter(
        DemoUserContext userContext,
        SupportTicketService supportTicketService,
        McpAuditLog auditLog
    ) {
        this.userContext = userContext;
        this.supportTicketService = supportTicketService;
        this.auditLog = auditLog;
    }

    /*
     * Tool 适合暴露“可被模型主动调用的动作”。
     * 这里不直接读取数据库，而是复用已有 SupportTicketService，让原来的租户和角色校验继续生效。
     */
    @McpTool(
        name = "get_ticket_context",
        title = "读取客服工单上下文",
        description = "读取当前坐席有权访问的工单摘要。不会返回内部备注。",
        generateOutputSchema = true
    )
    public TicketView getTicketContext(
        @McpToolParam(description = "工单号，例如 TCK-1001") String ticketId
    ) {
        CurrentUser user = userContext.currentUser();
        TicketView ticket = supportTicketService.getTicket(user, ticketId);
        auditLog.record(user.userId(), "get_ticket_context", ticketId, ticket.status().name());
        return ticket;
    }

    /*
     * 生成回复草稿仍然交给原业务服务处理。
     * MCP Adapter 只负责把它包装为 Tool，并把调用动作写入审计日志。
     */
    @McpTool(
        name = "draft_ticket_reply",
        title = "生成工单回复草稿",
        description = "基于已有 SupportTicketService 生成回复草稿，并标记是否需要人工审批。",
        generateOutputSchema = true
    )
    public ReplyDraft draftTicketReply(
        @McpToolParam(description = "工单号，例如 TCK-1001") String ticketId,
        @McpToolParam(description = "回复语气：warm、formal 或 concise") String tone
    ) {
        CurrentUser user = userContext.currentUser();
        ReplyDraft draft = supportTicketService.draftReply(user, ticketId, tone);
        auditLog.record(user.userId(), "draft_ticket_reply", ticketId, draft.needsHumanApproval() ? "approval_required" : "drafted");
        return draft;
    }

    /*
     * Resource 适合暴露“可读取的上下文资料”，例如政策、知识库片段、配置说明。
     * 这里通过 tenantId 模板化 URI，同时禁止跨租户读取。
     */
    @McpResource(
        uri = "support-policy://sla/{tenantId}",
        name = "support-sla-policy",
        title = "客服 SLA 政策",
        description = "读取当前租户的客服 SLA 和人工审批规则。",
        mimeType = "text/markdown"
    )
    public ReadResourceResult readSlaPolicy(
        @McpArg(name = "tenantId", description = "租户 ID，例如 acme", required = true) String tenantId
    ) {
        CurrentUser user = userContext.currentUser();
        if (!user.tenantId().equals(tenantId)) {
            throw new SecurityException("当前用户只能读取自己租户的 SLA 政策。");
        }
        String uri = "support-policy://sla/" + tenantId;
        String policy = supportTicketService.slaPolicy(user);
        return new ReadResourceResult(List.of(new TextResourceContents(uri, "text/markdown", policy)));
    }

    /*
     * Prompt 用来沉淀稳定的任务流程。
     * 客户端拿到 Prompt 后，会知道应该先读工单和 SLA，再生成可审阅的回复草稿。
     */
    @McpPrompt(
        name = "ticket_reply_copilot",
        title = "客服工单回复助手",
        description = "要求模型先读取工单上下文和 SLA 政策，再生成可审阅回复。"
    )
    public GetPromptResult ticketReplyCopilotPrompt(
        @McpArg(name = "ticketId", description = "待处理工单号", required = true) String ticketId
    ) {
        CurrentUser user = userContext.currentUser();
        String prompt = """
            你是客服工单回复助手。处理工单前必须：
            1. 调用 get_ticket_context 读取工单上下文。
            2. 读取 support-policy://sla/%s 获取当前租户 SLA。
            3. 如需生成草稿，调用 draft_ticket_reply。
            4. 不得暴露内部备注；needsHumanApproval=true 时只能给坐席审阅建议，不能自动发送。

            待处理工单：%s
            """.formatted(user.tenantId(), ticketId);

        return new GetPromptResult(
            "客服工单回复任务模板",
            List.of(new PromptMessage(Role.USER, new TextContent(prompt)))
        );
    }
}
