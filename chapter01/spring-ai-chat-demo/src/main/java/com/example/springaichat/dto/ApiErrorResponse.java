package com.example.springaichat.dto;

import java.time.Instant;

/**
 * Small and stable error shape for API clients.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}
