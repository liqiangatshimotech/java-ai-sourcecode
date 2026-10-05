package com.example.existingservicemcp;

import com.example.existingservicemcp.existing.ReplyDraft;
import com.example.existingservicemcp.existing.TicketStatus;
import com.example.existingservicemcp.existing.TicketView;
import com.example.existingservicemcp.mcp.McpAuditLog;
import com.example.existingservicemcp.mcp.SupportTicketMcpAdapter;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ExistingServiceToMcpTest {

    @Autowired
    private SupportTicketMcpAdapter mcpAdapter;

    @Autowired
    private McpAuditLog auditLog;

    @Test
    void exposesExistingTicketLookupAsMcpTool() {
        TicketView ticket = mcpAdapter.getTicketContext("TCK-1001");

        assertEquals("TCK-1001", ticket.ticketId());
        assertEquals(TicketStatus.OPEN, ticket.status());
        assertFalse(ticket.publicSummary().contains("内部备注"));
    }

    @Test
    void blocksCrossTenantTicketAccess() {
        assertThrows(SecurityException.class, () -> mcpAdapter.getTicketContext("TCK-2001"));
    }

    @Test
    void draftsReplyViaExistingService() {
        ReplyDraft draft = mcpAdapter.draftTicketReply("TCK-1001", "warm");

        assertEquals("TCK-1001", draft.ticketId());
        assertTrue(draft.needsHumanApproval());
        assertTrue(draft.reply().contains("理解"));
    }

    @Test
    void exposesSlaPolicyAsResource() {
        ReadResourceResult resource = mcpAdapter.readSlaPolicy("acme");
        TextResourceContents content = (TextResourceContents) resource.contents().getFirst();

        assertEquals("support-policy://sla/acme", content.uri());
        assertTrue(content.text().contains("Enterprise"));
    }

    @Test
    void exposesPromptForCopilotWorkflow() {
        GetPromptResult prompt = mcpAdapter.ticketReplyCopilotPrompt("TCK-1001");

        assertEquals("客服工单回复任务模板", prompt.description());
        assertTrue(prompt.messages().getFirst().content().toString().contains("get_ticket_context"));
    }

    @Test
    void writesMcpAuditLog() {
        mcpAdapter.getTicketContext("TCK-1002");

        assertTrue(auditLog.entries().stream().anyMatch(entry -> entry.contains("get_ticket_context")));
    }
}
