package com.example.supportticketcopilot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TicketAnalysis(
    @NotNull TicketCategory category,
    @NotNull TicketPriority priority,
    @NotBlank @Size(max = 80) String summary,
    @NotNull @Size(max = 6) List<@NotBlank @Size(max = 60) String> requiredData,
    @NotNull @Size(min = 1, max = 5) List<@Valid ActionItem> nextActions,
    @NotNull @Valid CustomerReplyDraft customerReply,
    @DecimalMin("0.0") @DecimalMax("1.0") double confidence
) {
}
