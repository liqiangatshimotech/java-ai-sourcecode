package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.TicketStatus;

public record TicketQuery(
    TicketStatus status,
    String customerId
) {
}
