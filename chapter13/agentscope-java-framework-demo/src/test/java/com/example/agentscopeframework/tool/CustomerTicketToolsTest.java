package com.example.agentscopeframework.tool;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CustomerTicketToolsTest {

    @Test
    void queriesOrderStatus() {
        CustomerTicketTools tools = new CustomerTicketTools();

        String result = tools.queryOrderStatus("ORDER-2002");

        assertTrue(result.contains("库存不足"));
    }

    @Test
    void searchesPolicy() {
        CustomerTicketTools tools = new CustomerTicketTools();

        String result = tools.searchPolicy("refund");

        assertTrue(result.contains("退款政策"));
    }

    @Test
    void createsEscalation() {
        CustomerTicketTools tools = new CustomerTicketTools();

        String result = tools.createEscalation("T-1", "VIP complaint");

        assertTrue(result.contains("T-1"));
        assertTrue(result.contains("VIP complaint"));
    }
}
