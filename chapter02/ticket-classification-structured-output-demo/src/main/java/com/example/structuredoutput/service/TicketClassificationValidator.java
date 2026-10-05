package com.example.structuredoutput.service;

import com.example.structuredoutput.dto.TicketClassification;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

public class TicketClassificationValidator {

    private final Validator validator;

    public TicketClassificationValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(TicketClassification result) {
        Set<ConstraintViolation<TicketClassification>> violations =
            validator.validate(result);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("模型结构化输出不合法：" + message);
        }
    }
}
