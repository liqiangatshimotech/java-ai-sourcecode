package com.example.langchain4jchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        String provider,

        @NotBlank(message = "message must not be blank")
        @Size(max = 8000, message = "message must be no longer than 8000 characters")
        String message
) {
}
