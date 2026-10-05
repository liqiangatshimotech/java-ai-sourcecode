package com.example.supportticketcopilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnalyzeTicketRequest(
    @NotBlank @Size(max = 4000) String content,
    @NotNull CustomerChannel channel,
    @NotNull CustomerTier customerTier
) {

    public AnalyzeTicketRequest {
        if (channel == null) {
            channel = CustomerChannel.WEB;
        }
        if (customerTier == null) {
            customerTier = CustomerTier.STANDARD;
        }
    }
}
