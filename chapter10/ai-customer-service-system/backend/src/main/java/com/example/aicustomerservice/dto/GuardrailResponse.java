package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.ProductionGuardrail;

public record GuardrailResponse(
    String section,
    String problem,
    String signal,
    String action,
    boolean requiresHandoff
) {
    public static GuardrailResponse from(ProductionGuardrail guardrail) {
        return new GuardrailResponse(
            guardrail.problem().section(),
            guardrail.problem().title(),
            guardrail.signal(),
            guardrail.action(),
            guardrail.requiresHandoff()
        );
    }
}
