package com.example.supportticketcopilot.service;

import com.example.supportticketcopilot.dto.TicketAnalysis;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class TicketAnalysisValidator {

    private final Validator validator;

    public TicketAnalysisValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(TicketAnalysis analysis) {
        Objects.requireNonNull(analysis, "ticket analysis must not be null");

        Set<ConstraintViolation<TicketAnalysis>> violations = validator.validate(analysis);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException("Ticket analysis validation failed", violations);
        }
    }
}
