package com.example.agentscopeframework.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agentscope.core.message.Msg;
import org.junit.jupiter.api.Test;

class ServiceTicketProjectAgentTest {

    @Test
    void buildsPromptFromBusinessTicket() {
        ServiceTicket ticket = ServiceTicketProjectAgent.sampleTicket();

        Msg request = ServiceTicketProjectAgent.buildRequest(ticket);
        String prompt = request.getTextContent();

        assertTrue(prompt.contains("ORDER-2002"));
        assertTrue(prompt.contains("query_order_status"));
        assertTrue(prompt.contains("TicketDecision"));
    }

    @Test
    void fallbackDecisionKeepsBusinessPriority() {
        ServiceTicket ticket = ServiceTicketProjectAgent.sampleTicket();

        TicketDecision decision = ServiceTicketProjectAgent.fallbackDecision(ticket);

        assertEquals(ticket.priority(), decision.priority);
        assertTrue(decision.needHumanReview);
        assertTrue(decision.nextActions.stream().anyMatch(action -> action.contains("ORDER-2002")));
    }
}
