package com.example.agentscope2.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CustomerTicketToolsTest {

    @Test
    void queriesTicketSummary() {
        CustomerTicketTools tools = new CustomerTicketTools();

        assertTrue(tools.queryTicketSummary("T-1001").contains("504"));
    }

    @Test
    void suggestsOwnerTeam() {
        CustomerTicketTools tools = new CustomerTicketTools();

        assertEquals("平台稳定性小组", tools.suggestTicketOwner("DeepSeek 网关 504"));
        assertEquals("商务支持小组", tools.suggestTicketOwner("发票抬头修改"));
    }
}
