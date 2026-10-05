package com.example.springaimcpclient.web;

import jakarta.validation.constraints.NotBlank;

public record SupportQuestionRequest(
    String provider,
    @NotBlank String question
) {
}
