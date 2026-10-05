package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.SupportTicket;
import java.util.List;
import java.util.Optional;

public interface TicketDao {

    SupportTicket save(SupportTicket ticket);

    Optional<SupportTicket> findByTicketId(String ticketId);

    Optional<SupportTicket> findByIdempotencyKey(String idempotencyKey);

    List<SupportTicket> search(TicketQuery query);
}
