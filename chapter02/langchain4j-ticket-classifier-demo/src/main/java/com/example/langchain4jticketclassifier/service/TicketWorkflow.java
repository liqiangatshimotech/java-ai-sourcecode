package com.example.langchain4jticketclassifier.service;

import com.example.langchain4jticketclassifier.assistant.TicketClassifierAssistant;
import com.example.langchain4jticketclassifier.dto.TicketClassification;
import com.example.langchain4jticketclassifier.dto.TicketPriority;

public class TicketWorkflow {

    private final TicketClassifierAssistant assistant;
    private final TicketClassificationValidator validator;

    public TicketWorkflow(
        TicketClassifierAssistant assistant,
        TicketClassificationValidator validator
    ) {
        this.assistant = assistant;
        this.validator = validator;
    }

    public TicketClassification classifyAndRoute(String ticketText) {
        TicketClassification result = this.assistant.classify(ticketText);
        this.validator.validate(result);

        if (result.priority() == TicketPriority.URGENT) {
            System.out.println("命中紧急工单，可在这里升级到值班通知或人工审核。");
        }

        return result;
    }
}
