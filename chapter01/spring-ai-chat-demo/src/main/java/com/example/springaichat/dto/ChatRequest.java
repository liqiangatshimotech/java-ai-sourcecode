package com.example.springaichat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /chat.
 *
 * @param message user question or instruction
 * @param provider optional provider name: ollama or openai
 */
public record ChatRequest(
        @NotBlank(message = "message must not be blank")
        @Size(max = 8000, message = "message must be no longer than 8000 characters")
        String message,

        String provider) {
}
