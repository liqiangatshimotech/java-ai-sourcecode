package com.example.supportticketcopilot.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.ticket-copilot")
public record TicketCopilotProperties(
    @Min(1) @Max(5) int maxAttempts
) {

    public TicketCopilotProperties {
        if (maxAttempts == 0) {
            maxAttempts = 2;
        }
    }
}
