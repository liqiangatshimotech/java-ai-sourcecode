package com.example.structuredoutput.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TicketClassification(
    @NotNull TicketCategory category,
    @NotNull TicketPriority priority,
    @NotBlank @Size(max = 80) String summary,
    @NotNull List<@NotBlank String> requiredData,
    @DecimalMin("0.0") @DecimalMax("1.0") double confidence
) {
}
