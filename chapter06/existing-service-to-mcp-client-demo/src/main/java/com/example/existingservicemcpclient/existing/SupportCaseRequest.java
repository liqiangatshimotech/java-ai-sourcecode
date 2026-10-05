package com.example.existingservicemcpclient.existing;

import jakarta.validation.constraints.NotBlank;

public record SupportCaseRequest(
    @NotBlank String orderId,
    @NotBlank String customerQuestion
) {
}
