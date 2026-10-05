package com.example.springairag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AskQuestionRequest(
    @NotBlank(message = "question must not be blank")
    String question,
    String tenantId,
    @Positive(message = "topK must be positive")
    Integer topK
) {
}
