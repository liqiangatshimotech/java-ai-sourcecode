package com.example.supportticketcopilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActionItem(
    @NotNull OwnerTeam ownerTeam,
    @NotBlank @Size(max = 120) String description,
    @NotBlank @Size(max = 80) String nextStep
) {
}
