package com.example.aicustomerservice.entity;

public record ProductionGuardrail(
    EnterpriseProblemType problem,
    String signal,
    String action,
    boolean requiresHandoff
) {
}
