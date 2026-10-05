package com.example.structuredoutput.service;

import com.example.structuredoutput.model.ModelClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

public final class DemoFactory {

    private DemoFactory() {
    }

    public static TicketClassificationService createService(ModelClient modelClient, int maxAttempts) {
        ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Validator jakartaValidator =
            Validation.buildDefaultValidatorFactory().getValidator();

        TicketClassificationValidator outputValidator =
            new TicketClassificationValidator(jakartaValidator);

        return new TicketClassificationService(
            modelClient,
            objectMapper,
            outputValidator,
            maxAttempts
        );
    }

    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
