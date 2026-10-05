package com.example.springaiticketclassifier.service;

import com.example.springaiticketclassifier.dto.TicketClassification;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TicketClassificationValidator {

    private final Validator validator;

    public TicketClassificationValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(TicketClassification result) {
        if (result == null) {
            throw new IllegalArgumentException("模型结构化输出为空");
        }

        Set<ConstraintViolation<TicketClassification>> violations =
            this.validator.validate(result);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("模型结构化输出不合法：" + message);
        }
    }
}
