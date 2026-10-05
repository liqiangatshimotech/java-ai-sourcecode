package com.example.supportticketcopilot.service;

import com.example.supportticketcopilot.dto.TicketAnalysis;
import reactor.core.publisher.Flux;

public interface TicketAiGateway {

    TicketAnalysis analyzeTicket(String prompt);

    Flux<String> streamCustomerReply(String prompt);
}
